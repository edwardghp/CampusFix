package com.campusfix.domain.model

/**
 * HU09 - Modelo para los mensajes del chat del asistente IA.
 */
data class ChatMessage(
    val id: String = "",
    val ticketId: String,
    val role: String, // "user" o "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
