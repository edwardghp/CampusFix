package com.campusfix.data.remote

import com.google.gson.annotations.SerializedName


data class GeminiRequest(
    val contents: List<Content>,
    @SerializedName("system_instruction") val systemInstruction: SystemInstruction? = null,
    @SerializedName("generationConfig") val generationConfig: GenerationConfig? = null,
)

data class SystemInstruction(
    val parts: List<Part>
)

data class Content(
    val parts: List<Part>,
    val role: String = "user",
)

/**
 * Una "part" puede ser texto o datos en linea (la imagen en base64).
 * Solo uno de los dos campos va relleno por cada part.
 */
data class Part(
    val text: String? = null,
    @SerializedName("inlineData") val inlineData: InlineData? = null,
)

data class InlineData(
    @SerializedName("mimeType") val mimeType: String,
    val data: String, // imagen codificada en base64
)

data class GenerationConfig(
    val temperature: Float = 0.2f,
    @SerializedName("responseMimeType") val responseMimeType: String = "application/json",
)

/* ---------- Response ---------- */

data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    @SerializedName("promptFeedback") val promptFeedback: PromptFeedback? = null,
)

data class Candidate(
    val content: Content? = null,
    @SerializedName("finishReason") val finishReason: String? = null,
)

data class PromptFeedback(
    @SerializedName("blockReason") val blockReason: String? = null,
)
