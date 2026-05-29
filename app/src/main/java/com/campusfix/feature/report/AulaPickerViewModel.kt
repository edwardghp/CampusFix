package com.campusfix.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.Aula
import com.campusfix.domain.repository.AulaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HU03 - ViewModel de la seleccion manual de aula.
 * Permite buscar y elegir un aula del catalogo local (Room) cuando no hay
 * un codigo QR disponible. La lista se mantiene sincronizada con Firestore.
 */
@HiltViewModel
class AulaPickerViewModel @Inject constructor(
    private val aulaRepository: AulaRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    /** Lista de aulas filtrada por el texto de busqueda. */
    val aulas: StateFlow<List<Aula>> =
        combine(aulaRepository.observeAulas(), _query) { lista, q ->
            if (q.isBlank()) {
                lista
            } else {
                lista.filter {
                    it.codigo.contains(q, ignoreCase = true) ||
                        it.nombre.contains(q, ignoreCase = true) ||
                        it.facultad.contains(q, ignoreCase = true) ||
                        it.edificio.contains(q, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        // Al abrir la pantalla, intentamos refrescar el catalogo desde Firestore
        sync()
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Descarga el catalogo de aulas de Firestore y lo guarda en Room. */
    fun sync() {
        viewModelScope.launch {
            _syncing.value = true
            aulaRepository.syncAulas()
            _syncing.value = false
        }
    }
}
