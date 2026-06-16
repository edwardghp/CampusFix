package com.campusfix.domain.usecase

import android.net.Uri
import com.campusfix.domain.model.DiagnosticoIA
import com.campusfix.domain.repository.DiagnosticoRepository
import javax.inject.Inject

/**
 * HU05 - Caso de uso: diagnosticar la falla con IA.
 * Encapsula la regla de negocio (debe haber una descripcion) antes de
 * llamar al repositorio. Lo usa el ReportViewModel.
 */
class DiagnosticarFallaUseCase @Inject constructor(
    private val repository: DiagnosticoRepository,
) {
    suspend operator fun invoke(descripcion: String, foto: Uri?): Result<DiagnosticoIA> {
        if (descripcion.isBlank()) {
            return Result.failure(Exception("Escribe una descripcion para que la IA analice la falla"))
        }
        return repository.diagnosticar(descripcion, foto)
    }
}
