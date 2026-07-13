package com.campusfix.feature.technician

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.repository.TicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketDetailUiState(
    val loading: Boolean = true,
    val ticket: Ticket? = null,
    val solucionDescripcion: String = "",
    val tiempoEmpleado: String = "", // texto del campo numerico (minutos)
    val fotoSolucion: Uri? = null,
    val guardando: Boolean = false,
    val error: String? = null,
    val cerrado: Boolean = false,
)

/**
 * HU08 - ViewModel de la pantalla de cierre del ticket (lado del tecnico).
 * Recibe el id del ticket por navegacion (SavedStateHandle), carga sus datos
 * y gestiona el formulario de solucion: descripcion, tiempo empleado y foto.
 */
@HiltViewModel
class TicketDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ticketRepository: TicketRepository,
) : ViewModel() {

    private val ticketId: String = checkNotNull(savedStateHandle["ticketId"])

    private val _state = MutableStateFlow(TicketDetailUiState())
    val state: StateFlow<TicketDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val ticket = ticketRepository.getTicketById(ticketId)
            _state.update {
                it.copy(
                    loading = false,
                    ticket = ticket,
                    error = if (ticket == null) "No se pudo cargar el ticket" else null,
                )
            }
        }
    }

    fun onDescripcion(v: String) = _state.update { it.copy(solucionDescripcion = v) }

    /** Solo permite digitos: el tiempo empleado se guarda en minutos. */
    fun onTiempo(v: String) = _state.update { it.copy(tiempoEmpleado = v.filter { c -> c.isDigit() }) }

    fun onFoto(uri: Uri) = _state.update { it.copy(fotoSolucion = uri) }

    fun consumeError() = _state.update { it.copy(error = null) }

    /** Valida el formulario y registra la solucion: el ticket pasa a estado RESUELTO. */
    fun registrarSolucion() {
        val s = _state.value
        if (s.solucionDescripcion.isBlank()) {
            _state.update { it.copy(error = "Describe la solucion aplicada") }
            return
        }
        val minutos = s.tiempoEmpleado.toIntOrNull()
        if (minutos == null || minutos <= 0) {
            _state.update { it.copy(error = "Indica el tiempo empleado en minutos") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(guardando = true, error = null) }
            ticketRepository.resolverTicket(
                ticketId = ticketId,
                solucionDescripcion = s.solucionDescripcion.trim(),
                tiempoEmpleadoMinutos = minutos,
                fotoSolucion = s.fotoSolucion,
            ).onSuccess {
                _state.update { it.copy(guardando = false, cerrado = true) }
            }.onFailure { e ->
                _state.update {
                    it.copy(guardando = false, error = e.message ?: "Error al registrar la solucion")
                }
            }
        }
    }
}
