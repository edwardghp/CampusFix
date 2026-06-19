package com.campusfix.domain.repository

import android.net.Uri
import com.campusfix.domain.model.DiagnosticoIA

/**
 * HU05 - Repositorio del diagnostico con IA.
 * Envia la descripcion (y opcionalmente una foto) a la API de Gemini y
 * devuelve la categoria, urgencia y diagnostico sugeridos.
 */
interface DiagnosticoRepository {
    suspend fun diagnosticar(descripcion: String, foto: Uri?): Result<DiagnosticoIA>
}
