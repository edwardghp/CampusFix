package com.campusfix.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.User
import com.campusfix.domain.model.UserRole
import com.campusfix.domain.repository.AuthRepository
import com.campusfix.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de la pantalla de autenticacion (patron MVVM + StateFlow). */
data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val success: Boolean = false,
)

/** HU01 - ViewModel de login y registro. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun consumeMessages() = _state.update { it.copy(error = null, info = null) }

    fun login(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            authRepository.login(email, password)
                .onSuccess { _state.update { s -> s.copy(loading = false, success = true) } }
                .onFailure { e -> _state.update { s -> s.copy(loading = false, error = e.message) } }
        }
    }

    /** Registra al usuario y crea su perfil inicial en Firestore. */
    fun register(email: String, password: String, nombre: String, rol: UserRole) {
        if (!validate(email, password)) return
        if (nombre.isBlank()) {
            _state.update { it.copy(error = "Ingresa tu nombre completo") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            authRepository.register(email, password)
                .onSuccess { fbUser ->
                    val perfil = User(
                        uid = fbUser.uid,
                        email = email.trim(),
                        nombre = nombre.trim(),
                        rol = rol,
                    )
                    profileRepository.saveProfile(perfil)
                    _state.update {
                        it.copy(
                            loading = false,
                            success = true,
                            info = "Cuenta creada. Revisa tu correo para verificarla.",
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            authRepository.loginWithGoogle(idToken)
                .onSuccess { fbUser ->
                    // Si es la primera vez con Google, creamos un perfil basico
                    profileRepository.saveProfile(
                        User(uid = fbUser.uid, email = fbUser.email ?: "",
                            nombre = fbUser.displayName ?: "")
                    )
                    _state.update { it.copy(loading = false, success = true) }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(error = "Escribe tu correo para recuperar la contrasena") }
            return
        }
        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
                .onSuccess { _state.update { it.copy(info = "Te enviamos un correo de recuperacion") } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    private fun validate(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Completa correo y contrasena") }
            return false
        }
        if (password.length < 6) {
            _state.update { it.copy(error = "La contrasena debe tener al menos 6 caracteres") }
            return false
        }
        return true
    }
}
