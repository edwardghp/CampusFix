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
                val tickets = snap?.documents?.mapNotNull { it.toTicket() } ?: emptyList()
                trySend(tickets)
                // HU07 - Cachear la cola del tecnico en Room para acceso offline
                if (tickets.isNotEmpty()) {
                    scope.launch { ticketDao.upsertAll(tickets.map { TicketEntity.from(it) }) }
                }
            }
        awaitClose { listener.remove() }
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

    override suspend fun updateTicketStatus(ticketId: String, nuevoEstado: TicketStatus): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        firestore.collection(Constants.COL_TICKETS).document(ticketId)
            .update(mapOf("estado" to nuevoEstado.name, "actualizadoEn" to now))
            .await()

        ticketDao.findById(ticketId)?.let { entity ->
            ticketDao.update(entity.copy(estado = nuevoEstado, actualizadoEn = now, sincronizado = true))
        }

        notificarReportante(ticketId, nuevoEstado)
    }

    /** HU07 - Envia un push al reportante del ticket informando el nuevo estado. */
    private suspend fun notificarReportante(ticketId: String, nuevoEstado: TicketStatus) {
        val ticket = ticketDao.findById(ticketId)?.toDomain() ?: runCatching {
            firestore.collection(Constants.COL_TICKETS).document(ticketId).get().await().toTicket()
        }.getOrNull() ?: return

        val reportante = userRepository.getUserById(ticket.reportanteUid) ?: return
        if (reportante.fcmToken.isBlank()) return

        fcmSender.sendNotification(
            token = reportante.fcmToken,
            title = "Actualizacion de tu ticket",
            body = "Tu reporte en ${ticket.aulaNombre} cambio a: ${nuevoEstado.label}."
        )
    }
}
