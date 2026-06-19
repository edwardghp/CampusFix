package com.campusfix.domain.model

/**
 * HU05 - Resultado del diagnostico automatico de la falla generado por la IA.
 *
 * La IA analiza la foto + la descripcion del problema y devuelve:
 *  - la categoria de falla sugerida,
 *  - el nivel de urgencia sugerido,
 *  - un diagnostico breve en texto.
 */
data class DiagnosticoIA(
    val categoria: FaultCategory,
    val urgencia: Urgency,
    val diagnostico: String,
)
