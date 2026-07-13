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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.campusfix.core.Constants
import com.campusfix.domain.model.TicketStatus
import com.campusfix.domain.model.User
import com.campusfix.domain.model.FaultCategory
import com.campusfix.domain.model.Urgency
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
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
 */
@Singleton
class TicketRepositoryImpl @Inject constructor(
    private val ticketDao: TicketDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val fcmSender: FcmSender,
    @ApplicationContext private val context: Context,
) : TicketRepository {

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

    override fun observeOpenTickets(): Flow<List<Ticket>> = callbackFlow {
        val listener = firestore.collection(Constants.COL_TICKETS)
            .whereEqualTo("estado", TicketStatus.ABIERTO.name)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tickets = snap?.documents?.mapNotNull { doc ->
                    try {
                        Ticket(
                            id = doc.id,
                            aulaId = doc.getString("aulaId") ?: "",
                            aulaNombre = doc.getString("aulaNombre") ?: "",
                            categoria = FaultCategory.valueOf(doc.getString("categoria") ?: "OTRO"),
                            urgencia = Urgency.valueOf(doc.getString("urgencia") ?: "MEDIA"),
                            descripcion = doc.getString("descripcion") ?: "",
                            fotoUrls = (doc.get("fotoUrls") as? List<String>) ?: emptyList(),
                            audioUrl = doc.getString("audioUrl") ?: "",
                            reportanteUid = doc.getString("reportanteUid") ?: "",
                            estado = TicketStatus.valueOf(doc.getString("estado") ?: "ABIERTO"),
                            creadoEn = doc.getLong("creadoEn") ?: 0L,
                            sincronizado = true
                        )
                    } catch (e: Exception) { null }
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
            "estado" to TicketStatus.ASIGNADO.name
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
                sincronizado = true
            ))
        }

        // --- HU06: Notificación Push Automática ---
        if (technician.fcmToken.isNotBlank()) {
            fcmSender.sendNotification(
                token = technician.fcmToken,
                title = "Nueva tarea asignada",
                body = "Se te ha asignado un reporte en ${technician.nombre}. Revisa 'Mis tareas'."
            )
        }
    }

    override fun observeAssignedTickets(uid: String): Flow<List<Ticket>> = callbackFlow {
        val listener = firestore.collection(Constants.COL_TICKETS)
            .whereEqualTo("tecnicoId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tickets = snap?.documents?.mapNotNull { doc -> docToTicket(doc.id, doc.data ?: return@mapNotNull null) }
                    ?: emptyList()
                trySend(tickets)
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