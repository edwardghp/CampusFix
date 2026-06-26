package com.campusfix.domain.repository

import android.net.Uri
import com.campusfix.domain.model.Aula
import com.campusfix.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.model.TicketStatus

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

/** HU06 - Gestion de usuarios y tecnicos. */
interface UserRepository {
    /** Obtiene la lista de todos los tecnicos registrados. */
    fun observeTechnicians(): Flow<List<User>>
    /** Actualiza el estado activo/inactivo de un tecnico. */
    suspend fun updateTechnicianStatus(uid: String, active: Boolean): Result<Unit>
    /** Actualiza el token FCM del usuario actual. */
    suspend fun updateFcmToken(uid: String, token: String): Result<Unit>
    /** HU07 - Obtiene un usuario puntual por uid (para leer su fcmToken al notificar). */
    suspend fun getUserById(uid: String): User?
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
    /** HU07 - Version en tiempo real de "mis tickets" (listener de Firestore + cache en Room). */
    fun observeMyTicketsRealtime(uid: String): Flow<List<Ticket>>
    /** HU06 - Observa todos los tickets abiertos para el coordinador. */
    fun observeOpenTickets(): Flow<List<Ticket>>
    /** HU06 - Asigna un ticket a un tecnico. */
    suspend fun assignTicket(ticketId: String, technician: User): Result<Unit>
    /** HU07 - Observa los tickets asignados a un tecnico (cola del tecnico), con cache en Room. */
    fun observeAssignedTickets(uid: String): Flow<List<Ticket>>
    /** HU07 - Seguimiento en tiempo real de un ticket puntual (vista del reportante).
     *  Usa listener de Firestore cuando hay red y cae a Room cuando esta offline. */
    fun observeTicketRealtime(ticketId: String): Flow<Ticket?>
    /** HU07 - El tecnico avanza el estado del ticket (Asignado -> En atencion -> Resuelto).
     *  Notifica por FCM al reportante en cada cambio. */
    suspend fun updateTicketStatus(ticketId: String, nuevoEstado: TicketStatus): Result<Unit>
}
