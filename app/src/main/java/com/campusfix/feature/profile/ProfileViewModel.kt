package com.campusfix.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.User
import com.campusfix.domain.repository.AuthRepository
import com.campusfix.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val user: User = User(),
    val error: String? = null,
    val saved: Boolean = false,
)

/** HU02 - ViewModel del perfil de usuario. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        val fbUser = authRepository.currentUser()
        if (fbUser == null) {
            _state.update { it.copy(loading = false, error = "Sesion no valida") }
            return
        }
        viewModelScope.launch {
            profileRepository.observeProfile(fbUser.uid).collect { user ->
                _state.update {
                    it.copy(
                        loading = false,
                        user = user ?: User(uid = fbUser.uid, email = fbUser.email ?: ""),
                    )
                }
            }
        }
    }

    fun onNombreChange(v: String) = _state.update { it.copy(user = it.user.copy(nombre = v)) }
    fun onFacultadChange(v: String) = _state.update { it.copy(user = it.user.copy(facultad = v)) }
    fun onCargoChange(v: String) = _state.update { it.copy(user = it.user.copy(cargo = v)) }
    fun consumeError() = _state.update { it.copy(error = null) }

    /** Sube la foto seleccionada a Firebase Storage y actualiza la URL. */
    fun changePhoto(uri: Uri) {
        val uid = _state.value.user.uid
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            profileRepository.uploadProfilePhoto(uid, uri)
                .onSuccess { url ->
                    _state.update { it.copy(saving = false, user = it.user.copy(fotoUrl = url)) }
                }
                .onFailure { e ->
                    _state.update { it.copy(saving = false, error = e.message ?: "Error al subir la foto") }
                }
        }
    }

    fun save() {
        val user = _state.value.user
        if (user.nombre.isBlank()) {
            _state.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            profileRepository.saveProfile(user)
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e ->
                    _state.update { it.copy(saving = false, error = e.message ?: "Error al guardar") }
                }
        }
    }
}
