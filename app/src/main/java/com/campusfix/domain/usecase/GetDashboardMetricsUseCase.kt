package com.campusfix.domain.usecase

import com.campusfix.domain.model.DashboardMetrics
import com.campusfix.domain.model.SlaPolicy
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.model.TicketStatus
import com.campusfix.domain.model.Urgency
import com.campusfix.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * HU10 - Caso de uso: obtener las metricas del dashboard del coordinador.
 *
 * Observa todos los tickets y los transforma en un objeto [DashboardMetrics]
 * listo para la UI. Toda la logica de agregacion (conteos, promedios, SLA) vive
 * aqui, en la capa de dominio, para mantenerla testeable y fuera de la vista.
 */
class GetDashboardMetricsUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
) {
    operator fun invoke(): Flow<DashboardMetrics> =
        ticketRepository.observeAllTickets().map { tickets -> calcular(tickets) }

    /** Calcula las metricas a partir de la lista de tickets. Publico para poder testearlo. */
    fun calcular(tickets: List<Ticket>, ahora: Long = System.currentTimeMillis()): DashboardMetrics {
        if (tickets.isEmpty()) return DashboardMetrics()

        val conteoPorEstado = TicketStatus.entries.associateWith { estado ->
            tickets.count { it.estado == estado }
        }

        val abiertos = conteoPorEstado[TicketStatus.ABIERTO] ?: 0
        val enProceso = (conteoPorEstado[TicketStatus.ASIGNADO] ?: 0) +
            (conteoPorEstado[TicketStatus.EN_ATENCION] ?: 0)
        val resueltos = (conteoPorEstado[TicketStatus.RESUELTO] ?: 0) +
            (conteoPorEstado[TicketStatus.CERRADO] ?: 0)

        val vencidos = tickets.count { SlaPolicy.estaVencido(it, ahora) }

        // Tickets ya resueltos con tiempo de atencion registrado (HU08).
        val conTiempo = tickets.mapNotNull { it.tiempoEmpleadoMinutos }
        val tiempoPromedioMinutos =
            if (conTiempo.isEmpty()) 0 else conTiempo.average().toInt()

        // Cumplimiento de SLA sobre los tickets que ya tienen fecha de resolucion.
        val resueltosConFecha = tickets.filter { it.resueltoEn != null }
        val cumplimientoSlaPct = if (resueltosConFecha.isEmpty()) {
            0
        } else {
            val cumplen = resueltosConFecha.count { SlaPolicy.cumplioSla(it) }
            (cumplen * 100) / resueltosConFecha.size
        }

        // Tiempo promedio de atencion por urgencia (para el grafico de barras).
        val tiempoPorUrgencia = Urgency.entries.associateWith { urg ->
            val tiempos = tickets
                .filter { it.urgencia == urg }
                .mapNotNull { it.tiempoEmpleadoMinutos }
            if (tiempos.isEmpty()) 0 else tiempos.average().toInt()
        }

        return DashboardMetrics(
            abiertos = abiertos,
            vencidos = vencidos,
            enProceso = enProceso,
            resueltos = resueltos,
            total = tickets.size,
            conteoPorEstado = conteoPorEstado,
            tiempoPromedioMinutos = tiempoPromedioMinutos,
            cumplimientoSlaPct = cumplimientoSlaPct,
            tiempoPromedioPorUrgencia = tiempoPorUrgencia,
        )
    }
}
