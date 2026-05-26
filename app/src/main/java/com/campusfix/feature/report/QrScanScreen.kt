package com.campusfix.feature.report

import androidx.compose.runtime.Composable
import com.campusfix.feature.common.EnConstruccionScreen

@Composable
fun QrScanScreen(
    onAulaDetected: (String) -> Unit,
    onBack: () -> Unit,
) {
    EnConstruccionScreen(
        titulo = "Escanear aula",
        historia = "HU03 - Identificacion del aula por QR",
        onBack = onBack,
    )
}
