package com.campusfix.domain.repository

import android.net.Uri
import com.campusfix.domain.model.Aula
import com.campusfix.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import com.campusfix.domain.model.Ticket
/* Interfaces de repositorio (capa domain). La capa data las implementa. */

/** HU01 - Autenticacion con Firebase Auth. */
interface AuthRepository {
    fun currentUser(): FirebaseUser?
    suspend fun register(email: String, password: String): Result<FirebaseUser>
    suspend fun login(email: String, password: String): Result<FirebaseUser>
    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun logout()
}

/** HU02 - Perfil de usuario en Firestore + foto en Storage. */
interface ProfileRepository {
    suspend fun saveProfile(user: User): Result<Unit>
    fun observeProfile(uid: String): Flow<User?>
    suspend fun uploadProfilePhoto(uid: String, photo: Uri): Result<String>
}

/** HU03 - Catalogo de aulas (offline-first con Room). */
interface AulaRepository {
    fun observeAulas(): Flow<List<Aula>>
    suspend fun findByQr(qrCode: String): Aula?
    suspend fun getById(aulaId: String): Aula?
    /** Descarga el catalogo desde Firestore y lo guarda en Room. */
    suspend fun syncAulas(): Result<Unit>
}

/** HU04 - Creacion y consulta de tickets. */
interface TicketRepository {
    /** Guarda el ticket localmente (Room) y encola su envio con WorkManager. */
    suspend fun createTicket(ticket: Ticket, photos: List<Uri>, audio: Uri?): Result<String>
    fun observeMyTickets(uid: String): Flow<List<Ticket>>
}