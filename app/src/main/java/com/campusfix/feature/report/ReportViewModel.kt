package com.campusfix.feature.report

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.Aula
import com.campusfix.domain.model.FaultCategory
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.model.Urgency
import com.campusfix.domain.repository.AuthRepository
import com.campusfix.domain.repository.AulaRepository
import com.campusfix.domain.repository.TicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val aula: Aula? = null,
    val categoria: FaultCategory = FaultCategory.PROYECTOR,
    val urgencia: Urgency = Urgency.MEDIA,
    val descripcion: String = "",
    val fotos: List<Uri> = emptyList(),
    val audio: Uri? = null,
    val sending: Boolean = false,
    val error: String? = null,
    val sent: Boolean = false,
)

/**
 * HU04 - ViewModel del formulario de reporte de falla.
 * Recibe el id del aula como argumento de navegacion (SavedStateHandle).
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val aulaRepository: AulaRepository,
    private val ticketRepository: TicketRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    private val maxFotos = 3

    init {
        val aulaId: String? = savedStateHandle["aulaId"]
        if (aulaId != null) {
            viewModelScope.launch {
                val aula = aulaRepository.getById(aulaId)
                _state.update { it.copy(aula = aula) }
            }
        }
    }

    fun onCategoria(c: FaultCategory) = _state.update { it.copy(categoria = c) }
    fun onUrgencia(u: Urgency) = _state.update { it.copy(urgencia = u) }
    fun onDescripcion(v: String) = _state.update { it.copy(descripcion = v) }
    fun consumeError() = _state.update { it.copy(error = null) }

    fun addFoto(uri: Uri) {
        _state.update {
            if (it.fotos.size >= maxFotos) it.copy(error = "Maximo $maxFotos fotos")
            else it.copy(fotos = it.fotos + uri)
        }
    }

    fun removeFoto(uri: Uri) = _state.update { it.copy(fotos = it.fotos - uri) }

    fun setAudio(uri: Uri?) = _state.update { it.copy(audio = uri) }

    fun send() {
        val s = _state.value
        val aula = s.aula
        if (aula == null) {
            _state.update { it.copy(error = "Aula no identificada") }
            return
        }
        if (s.descripcion.isBlank()) {
            _state.update { it.copy(error = "Describe brevemente la falla") }
            return
        }
        val uid = authRepository.currentUser()?.uid
        if (uid == null) {
            _state.update { it.copy(error = "Sesion no valida") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            val ticket = Ticket(
                aulaId = aula.id,
                aulaNombre = "${aula.codigo} - ${aula.nombre}",
                categoria = s.categoria,
                urgencia = s.urgencia,
                descripcion = s.descripcion.trim(),
                reportanteUid = uid,
            )
            ticketRepository.createTicket(ticket, s.fotos, s.audio)
                .onSuccess { _state.update { it.copy(sending = false, sent = true) } }
                .onFailure { e ->
                    _state.update { it.copy(sending = false, error = e.message ?: "Error al enviar") }
                }
        }
    }
}
