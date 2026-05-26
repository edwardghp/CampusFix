package com.campusfix.data.repository

import com.campusfix.core.Constants
import com.campusfix.data.local.AulaDao
import com.campusfix.data.local.AulaEntity
import com.campusfix.domain.model.Aula
import com.campusfix.domain.repository.AulaRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AulaRepositoryImpl @Inject constructor(
    private val aulaDao: AulaDao,
    private val firestore: FirebaseFirestore,
) : AulaRepository {

    override fun observeAulas(): Flow<List<Aula>> =
        aulaDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun findByQr(qrCode: String): Aula? =
        aulaDao.findByQr(qrCode.trim())?.toDomain()

    override suspend fun getById(aulaId: String): Aula? =
        aulaDao.findById(aulaId)?.toDomain()

    override suspend fun syncAulas(): Result<Unit> = runCatching {
        val snapshot = firestore.collection(Constants.COL_AULAS).get().await()
        val aulas = snapshot.documents.map { doc ->
            AulaEntity(
                id = doc.id,
                codigo = doc.getString("codigo") ?: "",
                nombre = doc.getString("nombre") ?: "",
                facultad = doc.getString("facultad") ?: "",
                edificio = doc.getString("edificio") ?: "",
                qrCode = doc.getString("qrCode") ?: doc.id,
            )
        }
        if (aulas.isNotEmpty()) aulaDao.upsertAll(aulas)
    }
}
