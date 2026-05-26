package com.campusfix.feature.report

import androidx.compose.runtime.Composable
import com.campusfix.feature.common.EnConstruccionScreen

@Composable
fun ReportScreen(
    onTicketSent: () -> Unit,
    onBack: () -> Unit,
) {
    EnConstruccionScreen(
        titulo = "Reportar falla",
        historia = "HU04 - Captura de evidencia y envio del ticket",
        onBack = onBack,
    )
}
