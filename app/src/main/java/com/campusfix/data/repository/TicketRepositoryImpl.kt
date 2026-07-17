package com.campusfix.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.campusfix.data.local.TicketDao
import com.campusfix.data.local.TicketEntity
import com.campusfix.data.worker.TicketSyncWorker
import com.campusfix.data.util.FcmSender
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.repository.TicketRepository
import com.campusfix.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.campusfix.core.Constants
import com.campusfix.domain.model.TicketStatus
import com.campusfix.domain.model.User
import com.campusfix.domain.model.FaultCategory
import com.campusfix.domain.model.Urgency
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.storage.FirebaseStorage
/**
 * HU04 - Creacion de tickets offline-first.
 * 1) Guarda el ticket en Room (estado: no sincronizado).
 * 2) Encola un Worker que sube las fotos/audio a Storage y escribe en Firestore.
 *    Si no hay red, WorkManager reintenta automaticamente cuando vuelve la conexion.
 *
 * HU07 - Cola de trabajo del tecnico y seguimiento en tiempo real:
 *  - observeAssignedTickets: cola del tecnico con listener de Firestore + cache en Room (offline).
 *  - observeTicketRealtime: seguimiento de UN ticket puntual para el reportante (listener + fallback Room).
 *  - updateTicketStatus: el tecnico avanza el estado del ticket y se notifica al reportante por FCM.
 */
@Singleton
class TicketRepositoryImpl @Inject constructor(
    private val ticketDao: TicketDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val fcmSender: FcmSender,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
) : TicketRepository {

    /** Mapea un documento de Firestore a un Ticket de dominio. Centraliza el parseo (HU07). */
    private fun DocumentSnapshot.toTicket(): Ticket? = try {
        Ticket(
            id = id,
            aulaId = getString("aulaId") ?: "",
            aulaNombre = getString("aulaNombre") ?: "",
            aulaLat = getDouble("aulaLat"),
            aulaLng = getDouble("aulaLng"),
            categoria = FaultCategory.valueOf(getString("categoria") ?: "OTRO"),
            urgencia = Urgency.valueOf(getString("urgencia") ?: "MEDIA"),
            descripcion = getString("descripcion") ?: "",
            fotoUrls = (get("fotoUrls") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            audioUrl = getString("audioUrl") ?: "",
            reportanteUid = getString("reportanteUid") ?: "",
            tecnicoId = getString("tecnicoId"),
            tecnicoNombre = getString("tecnicoNombre"),
            fechaAsignacion = getLong("fechaAsignacion"),
            estado = TicketStatus.valueOf(getString("estado") ?: "ABIERTO"),
            creadoEn = getLong("creadoEn") ?: 0L,
            actualizadoEn = getLong("actualizadoEn") ?: getLong("creadoEn") ?: 0L,
            sincronizado = true,
        )
    } catch (e: Exception) {
        null
    }

    override suspend fun createTicket(
        ticket: Ticket,
        photos: List<Uri>,
        audio: Uri?,
    ): Result<String> = runCatching {
        val id = ticket.id.ifBlank { UUID.randomUUID().toString() }

        val entity = TicketEntity(
            id = id,
            aulaId = ticket.aulaId,
            aulaNombre = ticket.aulaNombre,
            aulaLat = ticket.aulaLat,
            aulaLng = ticket.aulaLng,
            categoria = ticket.categoria,
            urgencia = ticket.urgencia,
            descripcion = ticket.descripcion,
            fotosLocales = photos.joinToString("|") { it.toString() },
            audioLocal = audio?.toString() ?: "",
            reportanteUid = ticket.reportanteUid,
            tecnicoId = ticket.tecnicoId,
            tecnicoNombre = ticket.tecnicoNombre,
            fechaAsignacion = ticket.fechaAsignacion,
            estado = ticket.estado,
            creadoEn = ticket.creadoEn,
            actualizadoEn = ticket.creadoEn,
            sincronizado = false,
        )
        ticketDao.insert(entity)

        // Encolar el envio. Requiere red; WorkManager reintenta si falla.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<TicketSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(TicketSyncWorker.KEY_TICKET_ID to id))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TicketSyncWorker.TAG)
            .build()
        WorkManager.getInstance(context).enqueue(request)

        id
    }

    override fun observeMyTickets(uid: String): Flow<List<Ticket>> =
        ticketDao.observeByUser(uid).map { list -> list.map { it.toDomain() } }

    override fun observeMyTicketsRealtime(uid: String): Flow<List<Ticket>> = callbackFlow {
        val scope = this
        val listener = firestore.collection(Constants.COL_TICKETS)
            .whereEqualTo("reportanteUid", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    // Sin red: dejamos que la UI siga usando observeMyTickets (Room) como respaldo.
                    return@addSnapshotListener
                }
                val tickets = snap?.documents?.mapNotNull { it.toTicket() } ?: emptyList()
                trySend(tickets)
                if (tickets.isNotEmpty()) {
                    scope.launch { ticketDao.upsertAll(tickets.map { TicketEntity.from(it) }) }
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observeOpenTickets(): Flow<List<Ticket>> = callbackFlow {
        val listener = firestore.collection(Constants.COL_TICKETS)
            .whereEqualTo("estado", TicketStatus.ABIERTO.name)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toTicket() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    /**
     * HU10 - Escucha en tiempo real TODA la coleccion de tickets para alimentar el
     * dashboard del coordinador. Usa docToTicket() (y no toTicket()) porque este
     * parsea los campos de la HU08 (resueltoEn, tiempoEmpleadoMinutos) necesarios
     * para calcular tiempos de atencion y cumplimiento de SLA.
     */
    override fun observeAllTickets(): Flow<List<Ticket>> = callbackFlow {
        val listener = firestore.collection(Constants.COL_TICKETS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tickets = snap?.documents?.mapNotNull { doc ->
                    docToTicket(doc.id, doc.data ?: return@mapNotNull null)
                } ?: emptyList()
                trySend(tickets)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun assignTicket(ticketId: String, technician: User): Result<Unit> = runCatching {
        val update = mapOf(
            "tecnicoId" to technician.uid,
            "tecnicoNombre" to technician.nombre,
            "fechaAsignacion" to System.currentTimeMillis(),
            "estado" to TicketStatus.ASIGNADO.name,
            "actualizadoEn" to System.currentTimeMillis(),
        )
        firestore.collection(Constants.COL_TICKETS).document(ticketId)
            .update(update)
            .await()

        // Tambien actualizamos Room si el ticket existe localmente
        ticketDao.findById(ticketId)?.let { entity ->
            ticketDao.update(entity.copy(
                tecnicoId = technician.uid,
                tecnicoNombre = technician.nombre,
                fechaAsignacion = System.currentTimeMillis(),
                estado = TicketStatus.ASIGNADO,
                actualizadoEn = System.currentTimeMillis(),
                sincronizado = true
            ))
        }

        // --- HU06: Notificación Push Automática al tecnico ---
        if (technician.fcmToken.isNotBlank()) {
            fcmSender.sendNotification(
                token = technician.fcmToken,
                title = "Nueva tarea asignada",
                body = "Se te ha asignado un reporte en ${technician.nombre}. Revisa 'Mis tareas'."
            )
        }

        // --- HU07: Notificación Push al reportante por el cambio de estado ---
        notificarReportante(ticketId, TicketStatus.ASIGNADO)
    }

    /** HU07 - Notifica por FCM al reportante que el estado de su ticket cambio. */
    private suspend fun notificarReportante(ticketId: String, nuevoEstado: TicketStatus) {
        val ticketDoc = firestore.collection(Constants.COL_TICKETS).document(ticketId).get().await()
        val reportanteUid = ticketDoc.getString("reportanteUid") ?: return
        val aulaNombre = ticketDoc.getString("aulaNombre") ?: "tu aula"
        if (reportanteUid.isBlank()) return

        val reportanteDoc = firestore.collection(Constants.COL_USERS).document(reportanteUid).get().await()
        val fcmToken = reportanteDoc.getString("fcmToken") ?: ""
        if (fcmToken.isBlank()) return

        fcmSender.sendNotification(
            token = fcmToken,
            title = "Tu reporte cambio de estado",
            body = "Tu reporte en $aulaNombre ahora esta: ${nuevoEstado.label}.",
        )
    }

    override fun observeAssignedTickets(uid: String): Flow<List<Ticket>> = callbackFlow {
        val scope = this
        val listener = firestore.collection(Constants.COL_TICKETS)
            .whereEqualTo("tecnicoId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    // Sin red o error de Firestore: no cerramos el flow, Room sigue disponible
                    // a traves de observeMyTickets/observeByTechnician si la UI decide usarlo.
                    return@addSnapshotListener
                }
                val tickets = snap?.documents?.mapNotNull { doc -> docToTicket(doc.id, doc.data ?: return@mapNotNull null) }
                    ?: emptyList()
                trySend(tickets)
                // HU07 - Cachear la cola del tecnico en Room para acceso offline
                if (tickets.isNotEmpty()) {
                    scope.launch { ticketDao.upsertAll(tickets.map { TicketEntity.from(it) }) }
                }
            }
        awaitClose { listener.remove() }
    }

    /** HU07 - El tecnico avanza el estado del ticket; notifica al reportante por FCM. */
    override suspend fun updateTicketStatus(ticketId: String, nuevoEstado: TicketStatus): Result<Unit> = runCatching {
        val update = mapOf(
            "estado" to nuevoEstado.name,
            "actualizadoEn" to System.currentTimeMillis(),
        )
        firestore.collection(Constants.COL_TICKETS).document(ticketId)
            .update(update)
            .await()

        // Tambien actualizamos Room si el ticket existe localmente
        ticketDao.findById(ticketId)?.let { entity ->
            ticketDao.update(entity.copy(
                estado = nuevoEstado,
                actualizadoEn = System.currentTimeMillis(),
                sincronizado = true,
            ))
        }

        // --- HU07: Notificación Push al reportante por el cambio de estado ---
        notificarReportante(ticketId, nuevoEstado)
    }

    override fun observeTicketRealtime(ticketId: String): Flow<Ticket?> = callbackFlow {
        val scope = this
        // 1) Emitimos primero lo que haya en cache local (Room) para que la UI tenga algo
        //    de inmediato, incluso sin conexion.
        ticketDao.findById(ticketId)?.let { trySend(it.toDomain()) }

        // 2) Nos suscribimos al listener de Firestore para recibir cambios en tiempo real.
        val listener = firestore.collection(Constants.COL_TICKETS).document(ticketId)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                val ticket = doc.toTicket() ?: return@addSnapshotListener
                trySend(ticket)
                scope.launch { ticketDao.upsertAll(listOf(TicketEntity.from(ticket))) }
            }
        awaitClose { listener.remove() }
    }

    /* ===================== HU08 - Cierre del ticket ===================== */

    override suspend fun getTicketById(ticketId: String): Ticket? = try {
        val doc = firestore.collection(Constants.COL_TICKETS).document(ticketId).get().await()
        if (!doc.exists()) null else docToTicket(doc.id, doc.data ?: emptyMap())
    } catch (e: Exception) {
        null
    }

    override suspend fun resolverTicket(
        ticketId: String,
        solucionDescripcion: String,
        tiempoEmpleadoMinutos: Int,
        fotoSolucion: Uri?,
    ): Result<Unit> = runCatching {
        // 1) Subir la foto del equipo reparado (evidencia de la solucion)
        var solucionFotoUrl = ""
        if (fotoSolucion != null) {
            val ref = storage.reference.child("${Constants.STORAGE_TICKETS}/$ticketId/solucion.jpg")
            ref.putFile(fotoSolucion).await()
            solucionFotoUrl = ref.downloadUrl.await().toString()
        }
        val resueltoEn = System.currentTimeMillis()

        // 2) Leer el ticket para saber a quien notificar (reportante) y en que aula fue
        val ticketDoc = firestore.collection(Constants.COL_TICKETS).document(ticketId).get().await()
        val reportanteUid = ticketDoc.getString("reportanteUid") ?: ""
        val aulaNombre = ticketDoc.getString("aulaNombre") ?: "tu aula"

        // 3) Actualizar el ticket en Firestore: pasa a RESUELTO con la evidencia
        val update = mapOf(
            "solucionDescripcion" to solucionDescripcion,
            "solucionFotoUrl" to solucionFotoUrl,
            "tiempoEmpleadoMinutos" to tiempoEmpleadoMinutos,
            "resueltoEn" to resueltoEn,
            "estado" to TicketStatus.RESUELTO.name,
        )
        firestore.collection(Constants.COL_TICKETS).document(ticketId).update(update).await()

        // 4) Actualizar la copia local en Room (base de conocimiento offline)
        ticketDao.findById(ticketId)?.let { entity ->
            ticketDao.update(
                entity.copy(
                    solucionDescripcion = solucionDescripcion,
                    solucionFotoUrl = solucionFotoUrl,
                    tiempoEmpleadoMinutos = tiempoEmpleadoMinutos,
                    resueltoEn = resueltoEn,
                    estado = TicketStatus.RESUELTO,
                    sincronizado = true,
                )
            )
        }

        // 5) Notificar por FCM al reportante que su falla fue resuelta
        if (reportanteUid.isNotBlank()) {
            val reportanteDoc = firestore.collection(Constants.COL_USERS)
                .document(reportanteUid).get().await()
            val fcmToken = reportanteDoc.getString("fcmToken") ?: ""
            if (fcmToken.isNotBlank()) {
                fcmSender.sendNotification(
                    token = fcmToken,
                    title = "Tu reporte fue resuelto",
                    body = "El tecnico resolvio la falla en $aulaNombre. Puedes calificar la atencion recibida.",
                )
            }
        }
    }

    override suspend fun calificarYCerrarTicket(ticketId: String, calificacion: Int): Result<Unit> =
        runCatching {
            require(calificacion in 1..5) { "La calificacion debe estar entre 1 y 5 estrellas" }
            val update = mapOf(
                "calificacion" to calificacion,
                "estado" to TicketStatus.CERRADO.name,
            )
            firestore.collection(Constants.COL_TICKETS).document(ticketId).update(update).await()

            ticketDao.findById(ticketId)?.let { entity ->
                ticketDao.update(
                    entity.copy(
                        calificacion = calificacion,
                        estado = TicketStatus.CERRADO,
                        sincronizado = true,
                    )
                )
            }
        }

    /** Convierte un documento de Firestore (tickets) al modelo de dominio completo. */
    private fun docToTicket(id: String, data: Map<String, Any?>): Ticket? = try {
        Ticket(
            id = id,
            aulaId = data["aulaId"] as? String ?: "",
            aulaNombre = data["aulaNombre"] as? String ?: "",
            categoria = FaultCategory.valueOf(data["categoria"] as? String ?: "OTRO"),
            urgencia = Urgency.valueOf(data["urgencia"] as? String ?: "MEDIA"),
            descripcion = data["descripcion"] as? String ?: "",
            fotoUrls = (data["fotoUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            audioUrl = data["audioUrl"] as? String ?: "",
            reportanteUid = data["reportanteUid"] as? String ?: "",
            tecnicoId = data["tecnicoId"] as? String,
            tecnicoNombre = data["tecnicoNombre"] as? String,
            fechaAsignacion = (data["fechaAsignacion"] as? Number)?.toLong(),
            estado = TicketStatus.valueOf(data["estado"] as? String ?: "ASIGNADO"),
            creadoEn = (data["creadoEn"] as? Number)?.toLong() ?: 0L,
            solucionDescripcion = data["solucionDescripcion"] as? String ?: "",
            solucionFotoUrl = data["solucionFotoUrl"] as? String ?: "",
            tiempoEmpleadoMinutos = (data["tiempoEmpleadoMinutos"] as? Number)?.toInt(),
            resueltoEn = (data["resueltoEn"] as? Number)?.toLong(),
            calificacion = (data["calificacion"] as? Number)?.toInt(),
            sincronizado = true,
        )
    } catch (e: Exception) {
        null
    }
}