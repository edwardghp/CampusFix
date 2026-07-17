package com.campusfix.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * HU05 - Interfaz Retrofit para la API de Gemini (generateContent).
 * El endpoint recibe el contenido (texto + imagen en base64) y devuelve
 * la respuesta generada por el modelo multimodal.
 */
interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest,
    ): GeminiResponse
}
