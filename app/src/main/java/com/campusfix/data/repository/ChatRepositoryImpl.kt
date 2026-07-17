package com.campusfix.data.repository

import com.campusfix.BuildConfig
import com.campusfix.data.local.ChatDao
import com.campusfix.data.local.ChatMessageEntity
import com.campusfix.data.local.TicketDao
import com.campusfix.data.remote.Content
import com.campusfix.data.remote.GeminiApi
import com.campusfix.data.remote.GeminiRequest
import com.campusfix.data.remote.Part
import com.campusfix.data.remote.SystemInstruction
import com.campusfix.domain.model.ChatMessage
import com.campusfix.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val geminiApi: GeminiApi,
    private val chatDao: ChatDao,
    private val ticketDao: TicketDao,
) : ChatRepository {

    override fun observeChatHistory(ticketId: String): Flow<List<ChatMessage>> {
        return chatDao.observeByTicket(ticketId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun sendMessage(ticketId: String, content: String): Result<ChatMessage> = try {
        // 1. Guardar mensaje del usuario
        val userMsg = ChatMessage(ticketId = ticketId, role = "user", content = content)
        chatDao.insert(ChatMessageEntity.from(userMsg))

        // 2. Preparar el contexto (System Prompt + Historial)
        val ticket = ticketDao.findById(ticketId)
        val systemPrompt = """
            Eres un asistente de IA de soporte técnico para la universidad CampusFix.
            Tu objetivo es ayudar al técnico o docente a resolver la falla reportada.
            Contexto del ticket actual:
            - Categoría: ${ticket?.categoria?.label ?: "Desconocida"}
            - Descripción original: ${ticket?.descripcion ?: "Sin descripción"}
            
            Instrucciones de formato:
            1. Responde de manera concisa y profesional.
            2. Usa párrafos cortos.
            3. Si usas listas, usa guiones (-) simples.
            4. Evita negritas (**) o cursivas (_) excesivas; usa texto plano lo más posible.
            5. No uses bloques de código o tablas complejas.
        """.trimIndent()

        val systemInstruction = SystemInstruction(
            parts = listOf(Part(text = systemPrompt))
        )

        // 3. Obtener el historial de chat
        val history = chatDao.getByTicket(ticketId).map {
            Content(role = it.role, parts = listOf(Part(text = it.content)))
        }

        val request = GeminiRequest(
            contents = history,
            systemInstruction = systemInstruction
        )

        // 4. Llamar a Gemini API
        val response = geminiApi.generateContent(
            model = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request
        )

        val aiContent = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "Lo siento, no pude generar una respuesta en este momento."

        // 4. Guardar mensaje de la IA
        val aiMsg = ChatMessage(ticketId = ticketId, role = "model", content = aiContent)
        chatDao.insert(ChatMessageEntity.from(aiMsg))

        Result.success(aiMsg)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
