package com.campusfix.feature.technician

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.FaultCategory
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.model.TicketStatus
import com.campusfix.domain.model.Urgency
import com.campusfix.domain.repository.AuthRepository
import com.campusfix.domain.repository.TicketRepository
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

/** HU07 - Filtros disponibles para la cola del tecnico. `null` = sin filtro (todos). */
data class AssignedTicketsFilters(
    val estado: TicketStatus? = null,
    val categoria: FaultCategory? = null,
)

data class AssignedTicketsUiState(
    val loading: Boolean = true,
    val message: String? = null,
    val filters: AssignedTicketsFilters = AssignedTicketsFilters(),
)

/**
 * HU07 - Cola de trabajo del tecnico:
 *  - Lista en tiempo real (listener de Firestore via TicketRepository, con cache offline en Room).
 *  - Ordenada por prioridad (urgencia Alta > Media > Baja, luego mas antiguo primero).
 *  - Filtros por estado y categoria.
 *  - Permite avanzar el estado del ticket (Asignado -> En atencion -> Resuelto).
 */
@HiltViewModel
class AssignedTicketsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val ticketRepository: TicketRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AssignedTicketsUiState())
    val state: StateFlow<AssignedTicketsUiState> = _state.asStateFlow()

    private val _filters = MutableStateFlow(AssignedTicketsFilters())

    private val rawAssignedTickets: StateFlow<List<Ticket>> = authRepository.currentUser()?.let { user ->
        ticketRepository.observeAssignedTickets(user.uid)
    }?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    /** Cola ya filtrada y ordenada por prioridad, lista para la UI. */
    val assignedTickets: StateFlow<List<Ticket>> = combine(rawAssignedTickets, _filters) { tickets, filters ->
        tickets
            .filter { filters.estado == null || it.estado == filters.estado }
            .filter { filters.categoria == null || it.categoria == filters.categoria }
            .sortedWith(
                compareBy<Ticket> { urgencyRank(it.urgencia) }.thenByDescending { it.creadoEn }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            rawAssignedTickets.collect { _state.update { it.copy(loading = false) } }
        }
    }

    private fun urgencyRank(u: Urgency) = when (u) {
        Urgency.ALTA -> 0
        Urgency.MEDIA -> 1
        Urgency.BAJA -> 2
    }

    fun setEstadoFilter(estado: TicketStatus?) {
        _filters.update { it.copy(estado = estado) }
        _state.update { it.copy(filters = _filters.value) }
    }

    fun setCategoriaFilter(categoria: FaultCategory?) {
        _filters.update { it.copy(categoria = categoria) }
        _state.update { it.copy(filters = _filters.value) }
    }

    /** HU07 - Avanza el estado del ticket. Notifica por FCM al reportante. */
    fun avanzarEstado(ticket: Ticket) {
        val siguiente = when (ticket.estado) {
            TicketStatus.ASIGNADO -> TicketStatus.EN_ATENCION
            TicketStatus.EN_ATENCION -> TicketStatus.RESUELTO
            else -> return
        }
        viewModelScope.launch {
            ticketRepository.updateTicketStatus(ticket.id, siguiente)
                .onSuccess { _state.update { it.copy(message = "Ticket actualizado a: ${siguiente.label}") } }
                .onFailure { e -> _state.update { it.copy(message = e.message ?: "No se pudo actualizar el ticket") } }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
