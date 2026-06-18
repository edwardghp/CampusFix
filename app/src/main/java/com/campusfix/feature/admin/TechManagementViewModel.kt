package com.campusfix.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.User
import com.campusfix.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TechManagementUiState(
    val loading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class TechManagementViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TechManagementUiState())
    val state: StateFlow<TechManagementUiState> = _state.asStateFlow()

    val technicians: StateFlow<List<User>> = userRepository.observeTechnicians()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleStatus(user: User) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            userRepository.updateTechnicianStatus(user.uid, !user.activo)
                .onSuccess {
                    _state.update { it.copy(loading = false, message = "Estado de ${user.nombre} actualizado") }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, message = "Error: ${e.message}") }
                }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
