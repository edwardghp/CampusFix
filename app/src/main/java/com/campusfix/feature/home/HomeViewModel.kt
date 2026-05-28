package com.campusfix.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.User
import com.campusfix.domain.repository.AuthRepository
import com.campusfix.domain.repository.AulaRepository
import com.campusfix.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val user: User = User(),
    val loading: Boolean = true,
)

/** Pantalla principal: muestra el saludo por rol y los tickets propios del usuario. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val aulaRepository: AulaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        val fbUser = authRepository.currentUser()
        if (fbUser != null) {
            // Perfil
            viewModelScope.launch {
                profileRepository.observeProfile(fbUser.uid).collect { user ->
                    _state.update { it.copy(user = user ?: User(uid = fbUser.uid), loading = false) }
                }
            }
            // Sincronizar catalogo de aulas en segundo plano (HU03)
            viewModelScope.launch { aulaRepository.syncAulas() }
        }
    }

    fun logout() = authRepository.logout()
}
