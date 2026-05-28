package com.campusfix.data.repository

import android.net.Uri
import com.campusfix.core.Constants
import com.campusfix.domain.model.User
import com.campusfix.domain.model.UserRole
import com.campusfix.domain.repository.ProfileRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** HU02 - Perfil de usuario en Firestore + foto de perfil en Firebase Storage. */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : ProfileRepository {

    override suspend fun saveProfile(user: User): Result<Unit> = runCatching {
        val data = mapOf(
            "uid" to user.uid,
            "email" to user.email,
            "nombre" to user.nombre,
            "rol" to user.rol.name,
            "facultad" to user.facultad,
            "cargo" to user.cargo,
            "fotoUrl" to user.fotoUrl,
            // Los tecnicos quedan inactivos hasta que el coordinador los valida (Sprint 2)
            "activo" to (user.rol != UserRole.TECNICO),
        )
        firestore.collection(Constants.COL_USERS).document(user.uid).set(data).await()
    }

    override fun observeProfile(uid: String): Flow<User?> = callbackFlow {
        val ref = firestore.collection(Constants.COL_USERS).document(uid)
        val listener = ref.addSnapshotListener { snap, error ->
            if (error != null) { trySend(null); return@addSnapshotListener }
            if (snap != null && snap.exists()) {
                trySend(
                    User(
                        uid = snap.getString("uid") ?: uid,
                        email = snap.getString("email") ?: "",
                        nombre = snap.getString("nombre") ?: "",
                        rol = UserRole.fromName(snap.getString("rol")),
                        facultad = snap.getString("facultad") ?: "",
                        cargo = snap.getString("cargo") ?: "",
                        fotoUrl = snap.getString("fotoUrl") ?: "",
                        activo = snap.getBoolean("activo") ?: true,
                    )
                )
            } else trySend(null)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun uploadProfilePhoto(uid: String, photo: Uri): Result<String> =
        runCatching {
            val ref = storage.reference.child("${Constants.STORAGE_PROFILE}/$uid.jpg")
            ref.putFile(photo).await()
            ref.downloadUrl.await().toString()
        }
}
