package com.campusfix.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.campusfix.data.remote.gemini.Content
import com.campusfix.data.remote.gemini.GeminiApi
import com.campusfix.data.remote.gemini.GenerationConfig
import com.campusfix.data.remote.gemini.GeminiRequest
import com.campusfix.data.remote.gemini.InlineData
import com.campusfix.data.remote.gemini.Part
import com.campusfix.domain.model.DiagnosticoIA
import com.campusfix.domain.model.FaultCategory
import com.campusfix.domain.model.Urgency
import com.campusfix.domain.repository.DiagnosticoRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.campusfix.BuildConfig

/**
 * HU05 - Implementacion del diagnostico con IA usando la API de Gemini.
 *
 * Flujo:
 *  1. Construye un prompt que le pide a Gemini clasificar la falla y responder
 *     SOLO en formato JSON (categoria, urgencia, diagnostico).
 *  2. Si hay foto, la comprime y la adjunta en base64 (modelo multimodal).
 *  3. Llama a la API (Retrofit + Coroutines, en Dispatchers.IO).
 *  4. Parsea el JSON de la respuesta al modelo de dominio DiagnosticoIA.
 */
@Singleton
class DiagnosticoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiApi: GeminiApi,
    private val gson: Gson,
) : DiagnosticoRepository {

    override suspend fun diagnosticar(
        descripcion: String,
        foto: Uri?,
    ): Result<DiagnosticoIA> = withContext(Dispatchers.IO) {
        runCatching {
            // 1) Construir las "parts": primero el prompt de texto
            val parts = mutableListOf(Part(text = buildPrompt(descripcion)))

            // 2) Adjuntar la foto si existe
            foto?.let { uri ->
                val base64 = encodeImage(uri)
                if (base64 != null) {
                    parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64)))
                }
            }

            val request = GeminiRequest(
                contents = listOf(Content(parts = parts)),
                generationConfig = GenerationConfig(),
            )

            // 3) Llamar a la API
            val response = geminiApi.generateContent(
                model = MODEL,
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = request,
            )

            // 4) Extraer y parsear el texto JSON devuelto
            val rawText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull { it.text != null }
                ?.text
                ?: error("La IA no devolvio una respuesta valida")

            parseDiagnostico(rawText)
        }.recoverCatching { e ->
            throw Exception(traducirError(e))
        }
    }

    /** Prompt que fuerza una respuesta JSON con las categorias/urgencias validas. */
    private fun buildPrompt(descripcion: String): String {
        val categorias = FaultCategory.entries.joinToString(", ") { it.name }
        val urgencias = Urgency.entries.joinToString(", ") { it.name }
        return """
            Eres un asistente de soporte tecnico universitario. Analiza la falla
            reportada en un aula (descripcion y, si se adjunta, la foto) y clasificala.

            Descripcion del problema: "$descripcion"

            Responde UNICAMENTE con un objeto JSON valido, sin texto adicional ni
            marcas de codigo, con esta estructura exacta:
            {
              "categoria": uno de [$categorias],
              "urgencia": uno de [$urgencias],
              "diagnostico": "explicacion breve en espanol, maximo 2 oraciones"
            }
        """.trimIndent()
    }

    /** Comprime la imagen y la codifica en base64. */
    private fun encodeImage(uri: Uri): String? {
        return try {
            val bitmap: Bitmap = context.contentResolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return null
            val output = ByteArrayOutputStream()
            // Comprimir para no enviar imagenes demasiado grandes a la API
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /** Convierte el JSON devuelto por la IA en el modelo de dominio. */
    private fun parseDiagnostico(rawText: String): DiagnosticoIA {
        // La IA a veces envuelve el JSON en ```json ... ```; lo limpiamos.
        val clean = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val json = gson.fromJson(clean, JsonObject::class.java)
            ?: error("No se pudo interpretar la respuesta de la IA")

        val categoria = json.get("categoria")?.asString
            ?.let { runCatching { FaultCategory.valueOf(it) }.getOrNull() }
            ?: FaultCategory.OTRO

        val urgencia = json.get("urgencia")?.asString
            ?.let { runCatching { Urgency.valueOf(it) }.getOrNull() }
            ?: Urgency.MEDIA

        val diagnostico = json.get("diagnostico")?.asString
            ?: "No se pudo generar un diagnostico detallado."

        return DiagnosticoIA(categoria, urgencia, diagnostico)
    }

    private fun traducirError(e: Throwable): String = when {
        e.message?.contains("API key", true) == true ||
            e.message?.contains("API_KEY", true) == true ||
            e.message?.contains("400") == true -> "Clave de API invalida o solicitud incorrecta."
        e.message?.contains("403") == true -> "Acceso denegado a la API de Gemini (revisa la clave)."
        e.message?.contains("429") == true -> "Se alcanzo el limite de solicitudes. Intenta de nuevo en un momento."
        e.message?.contains("timeout", true) == true ||
            e.message?.contains("Unable to resolve host", true) == true ||
            e.message?.contains("network", true) == true -> "Sin conexion a internet."
        else -> e.message ?: "No se pudo obtener el diagnostico de la IA."
    }

    companion object {
        // Modelo multimodal (texto + imagen). Flash es rapido y economico.
        private const val MODEL = "gemini-2.5-flash"
    }
}
