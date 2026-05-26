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
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.repository.TicketRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TicketRepositoryImpl @Inject constructor(
    private val ticketDao: TicketDao,
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
}
