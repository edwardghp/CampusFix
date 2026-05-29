package com.campusfix.data.repository

import com.campusfix.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** HU01 - Implementacion de autenticacion con Firebase Auth. */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {

    override fun currentUser(): FirebaseUser? = auth.currentUser

    override suspend fun register(email: String, password: String): Result<FirebaseUser> =
        runCatching {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: error("No se pudo crear el usuario")
            // Verificacion de email antes del primer acceso
            user.sendEmailVerification().await()
            user
        }.mapError()

    override suspend fun login(email: String, password: String): Result<FirebaseUser> =
        runCatching {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            result.user ?: error("Credenciales invalidas")
        }.mapError()

    override suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user ?: error("No se pudo iniciar sesion con Google")
        }.mapError()

    override suspend fun sendEmailVerification(): Result<Unit> =
        runCatching {
            auth.currentUser?.sendEmailVerification()?.await()
            Unit
        }.mapError()

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        runCatching {
            auth.sendPasswordResetEmail(email.trim()).await()
            Unit
        }.mapError()

    override fun logout() = auth.signOut()

    /** Traduce las excepciones de Firebase a mensajes legibles para el usuario. */
    private fun <T> Result<T>.mapError(): Result<T> = recoverCatching { e ->
        throw Exception(
            when {
                e.message?.contains("password is invalid", true) == true -> "Contrasena incorrecta"
                e.message?.contains("no user record", true) == true -> "No existe una cuenta con ese correo"
                e.message?.contains("email address is already", true) == true -> "Ese correo ya esta registrado"
                e.message?.contains("badly formatted", true) == true -> "Correo con formato invalido"
                e.message?.contains("network", true) == true -> "Sin conexion a internet"
                else -> e.message ?: "Error de autenticacion"
            }
        )
    }
}
