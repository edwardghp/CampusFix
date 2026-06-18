package com.campusfix.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.model.User
import com.campusfix.domain.repository.TicketRepository
import com.campusfix.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssignmentUiState(
    val selectedTicket: Ticket? = null,
    val technicians: List<TechnicianWithLoad> = emptyList(),
    val assigning: Boolean = false,
    val message: String? = null,
)

data class TechnicianWithLoad(
    val user: User,
    val currentLoad: Int,
    val isRecommended: Boolean = false
)

@HiltViewModel
class AssignmentViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AssignmentUiState())
    val state: StateFlow<AssignmentUiState> = _state.asStateFlow()

    val openTickets: StateFlow<List<Ticket>> = ticketRepository.observeOpenTickets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Lista de tecnicos con su carga de trabajo calculada y sugerencia.
     */
    val techniciansWithLoad: StateFlow<List<TechnicianWithLoad>> = combine(
        userRepository.observeTechnicians(),
        ticketRepository.observeOpenTickets(), // Para calcular carga (aunque lo ideal seria observar todos los no resueltos)
        _state
    ) { technicians, tickets, state ->
        val selectedTicket = state.selectedTicket
        technicians.map { tech ->
            // En una app real, 'tickets' deberia incluir todos los tickets asignados, no solo los abiertos.
            // Aqui simplificamos usando la categoria para recomendar.
            val load = 0 // Simulado por ahora, requiere una query de tickets asignados
            TechnicianWithLoad(
                user = tech,
                currentLoad = load,
                isRecommended = selectedTicket != null && tech.especialidad == selectedTicket.categoria
            )
        }.sortedWith(compareBy({ !it.isRecommended }, { it.currentLoad }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTicket(ticket: Ticket?) {
        _state.update { it.copy(selectedTicket = ticket) }
    }

    fun assign(technician: User) {
        val ticket = _state.value.selectedTicket ?: return
        viewModelScope.launch {
            _state.update { it.copy(assigning = true, message = null) }
            ticketRepository.assignTicket(ticket.id, technician)
                .onSuccess {
                    _state.update { it.copy(assigning = false, selectedTicket = null, message = "Ticket asignado a ${technician.nombre}") }
                }
                .onFailure { e ->
                    _state.update { it.copy(assigning = false, message = "Error: ${e.message}") }
                }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
