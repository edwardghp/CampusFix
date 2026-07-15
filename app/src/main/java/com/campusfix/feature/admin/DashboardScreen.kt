package com.campusfix.feature.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusfix.domain.model.DashboardMetrics
import com.campusfix.domain.model.SlaPolicy
import com.campusfix.domain.model.TicketStatus
import com.campusfix.domain.model.Urgency

/**
 * HU10 - Dashboard de metricas y SLA (solo Coordinador).
 * Muestra el volumen de incidencias, la distribucion por estado, el tiempo
 * promedio de atencion y el cumplimiento del SLA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Coordinador de Soporte",
                style = MaterialTheme.typography.titleLarge,
            )

            // --- Tarjetas KPI: Abiertos y Vencidos ---
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard(
                    titulo = "Abiertos",
                    valor = metrics.abiertos,
                    acento = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                KpiCard(
                    titulo = "Vencidos",
                    valor = metrics.vencidos,
                    acento = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }

            // --- Tarjetas KPI secundarias: En proceso y Resueltos ---
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard(
                    titulo = "En proceso",
                    valor = metrics.enProceso,
                    acento = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
                KpiCard(
                    titulo = "Resueltos",
                    valor = metrics.resueltos,
                    acento = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                )
            }

            // --- Grafico: Ticket por estado ---
            SectionCard(titulo = "Ticket por estado") {
                if (metrics.total == 0) {
                    EmptyHint("Aun no hay tickets registrados.")
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        EstadoDonutChart(
                            conteoPorEstado = metrics.conteoPorEstado,
                            total = metrics.total,
                            modifier = Modifier.size(120.dp),
                        )
                        EstadoLegend(
                            conteoPorEstado = metrics.conteoPorEstado,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // --- Grafico: Tiempo promedio de atencion por urgencia ---
            SectionCard(titulo = "Tiempo promedio de atencion") {
                if (metrics.tiempoPromedioMinutos == 0) {
                    EmptyHint("Aun no hay tickets resueltos para promediar.")
                } else {
                    Text(
                        "Promedio general: ${formatoMinutos(metrics.tiempoPromedioMinutos)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.height(12.dp))
                    TiempoBarChart(
                        tiempoPorUrgencia = metrics.tiempoPromedioPorUrgencia,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                    )
                }
            }

            // --- Cumplimiento de SLA ---
            SectionCard(titulo = "Cumplimiento de SLA") {
                SlaCompliance(metrics = metrics)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ===================== Componentes de UI ===================== */

@Composable
private fun KpiCard(
    titulo: String,
    valor: Int,
    acento: Color,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(acento),
            )
            Column(Modifier.padding(16.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    valor.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = acento,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun EmptyHint(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
}

/** Grafico de dona con la distribucion de tickets por estado. */
@Composable
private fun EstadoDonutChart(
    conteoPorEstado: Map<TicketStatus, Int>,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val grosor = size.minDimension * 0.22f
        val diametro = size.minDimension - grosor
        val topLeft = Offset(
            (size.width - diametro) / 2f,
            (size.height - diametro) / 2f,
        )
        val arcSize = Size(diametro, diametro)
        var startAngle = -90f
        TicketStatus.entries.forEach { estado ->
            val count = conteoPorEstado[estado] ?: 0
            if (count > 0) {
                val sweep = 360f * (count.toFloat() / total.toFloat())
                drawArc(
                    color = estadoColor(estado),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = grosor),
                )
                startAngle += sweep
            }
        }
    }
}

/** Leyenda con el conteo de cada estado, junto al grafico de dona. */
@Composable
private fun EstadoLegend(
    conteoPorEstado: Map<TicketStatus, Int>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TicketStatus.entries.forEach { estado ->
            val count = conteoPorEstado[estado] ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(estadoColor(estado)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    estado.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Grafico de barras del tiempo promedio de atencion (minutos) por urgencia. */
@Composable
private fun TiempoBarChart(
    tiempoPorUrgencia: Map<Urgency, Int>,
    modifier: Modifier = Modifier,
) {
    val maxValor = (tiempoPorUrgencia.values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Urgency.entries.forEach { urgencia ->
            val minutos = tiempoPorUrgencia[urgencia] ?: 0
            val fraccion = minutos.toFloat() / maxValor.toFloat()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (minutos > 0) formatoMinutos(minutos) else "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(fraccion.coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(urgenciaColor(urgencia)),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(urgencia.label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Barra de progreso con el % de cumplimiento del SLA y los objetivos por urgencia. */
@Composable
private fun SlaCompliance(metrics: DashboardMetrics) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${metrics.cumplimientoSlaPct}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorCumplimiento(metrics.cumplimientoSlaPct),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "de tickets resueltos dentro del plazo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { metrics.cumplimientoSlaPct / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = colorCumplimiento(metrics.cumplimientoSlaPct),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Objetivos de resolucion:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Urgency.entries.forEach { urgencia ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(urgencia.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${SlaPolicy.objetivoHoras(urgencia)} h",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

/* ===================== Helpers ===================== */

private fun estadoColor(estado: TicketStatus): Color = when (estado) {
    TicketStatus.ABIERTO -> Color(0xFF64748B)
    TicketStatus.ASIGNADO -> Color(0xFF6366F1)
    TicketStatus.EN_ATENCION -> Color(0xFF1D4ED8)
    TicketStatus.RESUELTO -> Color(0xFF2E7D32)
    TicketStatus.CERRADO -> Color(0xFF94A3B8)
}

private fun urgenciaColor(urgencia: Urgency): Color = when (urgencia) {
    Urgency.ALTA -> Color(0xFFDC2626)
    Urgency.MEDIA -> Color(0xFFF59E0B)
    Urgency.BAJA -> Color(0xFF2E7D32)
}

private fun colorCumplimiento(pct: Int): Color = when {
    pct >= 80 -> Color(0xFF2E7D32)
    pct >= 50 -> Color(0xFFF59E0B)
    else -> Color(0xFFDC2626)
}

/** Convierte minutos a un texto legible (ej. 90 -> "1h 30m"). */
private fun formatoMinutos(min: Int): String {
    if (min < 60) return "${min}m"
    val horas = min / 60
    val restante = min % 60
    return if (restante == 0) "${horas}h" else "${horas}h ${restante}m"
}
