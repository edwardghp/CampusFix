package com.campusfix.data.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.campusfix.core.Constants
import com.campusfix.data.local.TicketDao
import com.campusfix.data.util.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * HU04 - Worker que sincroniza un ticket pendiente:
 *  - Sube las fotos y el audio a Firebase Storage.
 *  - Escribe el ticket en Cloud Firestore.
 *  - Marca el ticket como sincronizado en Room.
 *  - Muestra una notificacion local de confirmacion.
 * Si falla (p. ej. sin red), devuelve retry() y WorkManager lo reintenta.
 */
@HiltWorker
class TicketSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val ticketDao: TicketDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ticketId = inputData.getString(KEY_TICKET_ID) ?: return Result.failure()
        val ticket = ticketDao.findById(ticketId) ?: return Result.failure()
        if (ticket.sincronizado) return Result.success()

        return try {
            // 1) Subir fotos
            val fotoUrls = mutableListOf<String>()
            ticket.fotosLocales.split("|").filter { it.isNotBlank() }.forEachIndexed { i, path ->
                val ref = storage.reference
                    .child("${Constants.STORAGE_TICKETS}/$ticketId/foto_$i.jpg")
                ref.putFile(Uri.parse(path)).await()
                fotoUrls.add(ref.downloadUrl.await().toString())
            }
            // 2) Subir audio (si existe)
            var audioUrl = ""
            if (ticket.audioLocal.isNotBlank()) {
                val ref = storage.reference
                    .child("${Constants.STORAGE_TICKETS}/$ticketId/audio.m4a")
                ref.putFile(Uri.parse(ticket.audioLocal)).await()
                audioUrl = ref.downloadUrl.await().toString()
            }
            // 3) Escribir el ticket en Firestore
            val data = mapOf(
                "id" to ticket.id,
                "aulaId" to ticket.aulaId,
                "aulaNombre" to ticket.aulaNombre,
                "aulaLat" to ticket.aulaLat,
                "aulaLng" to ticket.aulaLng,
                "categoria" to ticket.categoria.name,
                "urgencia" to ticket.urgencia.name,
                "descripcion" to ticket.descripcion,
                "fotoUrls" to fotoUrls,
                "audioUrl" to audioUrl,
                "reportanteUid" to ticket.reportanteUid,
                "tecnicoId" to ticket.tecnicoId,
                "tecnicoNombre" to ticket.tecnicoNombre,
                "fechaAsignacion" to ticket.fechaAsignacion,
                "estado" to ticket.estado.name,
                "creadoEn" to ticket.creadoEn,
                "actualizadoEn" to ticket.creadoEn,
            )
            firestore.collection(Constants.COL_TICKETS).document(ticketId).set(data).await()

            // 4) Marcar como sincronizado en Room (con las URLs remotas ya subidas)
            ticketDao.update(
                ticket.copy(
                    sincronizado = true,
                    fotoUrlsRemotas = fotoUrls.joinToString("|"),
                    audioUrlRemoto = audioUrl,
                )
            )

            // 5) Notificacion local de confirmacion (HU04)
            NotificationHelper.show(
                applicationContext,
                "Ticket enviado",
                "Tu reporte en ${ticket.aulaNombre} fue registrado correctamente.",
            )
            Result.success()
        } catch (e: Exception) {
            // Sin red u otro error transitorio: reintentar mas tarde
            Result.retry()
        }
    }

    companion object {
        const val KEY_TICKET_ID = "ticket_id"
        const val TAG = "ticket_sync"
    }
}
