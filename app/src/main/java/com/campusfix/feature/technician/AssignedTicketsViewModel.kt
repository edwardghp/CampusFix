package com.campusfix.feature.technician

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.repository.AuthRepository
import com.campusfix.domain.repository.TicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AssignedTicketsUiState(
    val loading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class AssignedTicketsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val ticketRepository: TicketRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AssignedTicketsUiState())
    val state: StateFlow<AssignedTicketsUiState> = _state.asStateFlow()

    val assignedTickets: StateFlow<List<Ticket>> = authRepository.currentUser()?.let { user ->
        ticketRepository.observeAssignedTickets(user.uid)
    }?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) 
        ?: MutableStateFlow(emptyList())

    fun clearMessage() = _state.update { it.copy(message = null) }
}
