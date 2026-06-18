package com.campusfix.data.repository

import com.campusfix.core.Constants
import com.campusfix.domain.model.User
import com.campusfix.domain.model.UserRole
import com.campusfix.domain.model.FaultCategory
import com.campusfix.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU06 - Gestion de usuarios y tecnicos en Firestore.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun observeTechnicians(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection(Constants.COL_USERS)
            .whereEqualTo("rol", UserRole.TECNICO.name)
            // Eliminamos el filtro de activo=true temporalmente para que el coordinador vea a todos los tecnicos registrados
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snap?.documents?.mapNotNull { doc ->
                    User(
                        uid = doc.id,
                        email = doc.getString("email") ?: "",
                        nombre = doc.getString("nombre") ?: "",
                        rol = UserRole.fromName(doc.getString("rol")),
                        facultad = doc.getString("facultad") ?: "",
                        cargo = doc.getString("cargo") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: "",
                        activo = doc.getBoolean("activo") ?: false,
                        especialidad = doc.getString("especialidad")?.let { 
                            try { FaultCategory.valueOf(it) } catch(e: Exception) { null }
                        },
                        fcmToken = doc.getString("fcmToken") ?: ""
                    )
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateFcmToken(uid: String, token: String): Result<Unit> = runCatching {
        firestore.collection(Constants.COL_USERS).document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .await()
    }

    override suspend fun updateTechnicianStatus(uid: String, active: Boolean): Result<Unit> = runCatching {
        firestore.collection(Constants.COL_USERS).document(uid)
            .set(mapOf("activo" to active), SetOptions.merge())
            .await()
    }
}
