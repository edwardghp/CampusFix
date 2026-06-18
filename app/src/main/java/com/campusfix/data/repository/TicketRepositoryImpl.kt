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
                            tecnicoId = doc.getString("tecnicoId"),
                            tecnicoNombre = doc.getString("tecnicoNombre"),
                            fechaAsignacion = doc.getLong("fechaAsignacion"),
                            estado = TicketStatus.valueOf(doc.getString("estado") ?: "ASIGNADO"),
                            creadoEn = doc.getLong("creadoEn") ?: 0L,
                            sincronizado = true
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(tickets)
            }
        awaitClose { listener.remove() }
    }
}
