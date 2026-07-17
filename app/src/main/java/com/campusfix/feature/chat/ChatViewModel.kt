package com.campusfix.feature.chat

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.ChatMessage
import com.campusfix.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val isListening: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val app: Application,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val ticketId: String = checkNotNull(savedStateHandle["ticketId"])

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = repository.observeChatHistory(ticketId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(app)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _uiState.update { it.copy(isListening = false) }
                    }
                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No se escuchó nada, intenta de nuevo."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de audio denegado."
                            SpeechRecognizer.ERROR_NETWORK -> "Error de red."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de espera agotado."
                            else -> "Error de voz: $error"
                        }
                        _uiState.update { it.copy(isListening = false, error = message) }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onInputTextChange(matches[0])
                        }
                        _uiState.update { it.copy(isListening = false) }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun startListening() {
        if (_uiState.value.isListening) return

        viewModelScope.launch {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                // Pedir resultados parciales para que sea mas responsivo si se desea, 
                // pero por ahora mantenemos el flujo simple.
            }
            speechRecognizer?.cancel() // Cancelar cualquier sesion previa colgada
            speechRecognizer?.startListening(intent)
            _uiState.update { it.copy(isListening = true) }
        }
    }

    fun sendMessage() {
        val content = uiState.value.inputText.trim()
        if (content.isEmpty() || uiState.value.isTyping) return

        viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isTyping = true) }
            val result = repository.sendMessage(ticketId, content)
            result.onFailure { e ->
                _uiState.update { it.copy(error = "Error al enviar mensaje: ${e.message}") }
            }
            _uiState.update { it.copy(isTyping = false) }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        speechRecognizer?.destroy()
        super.onCleared()
    }
}
