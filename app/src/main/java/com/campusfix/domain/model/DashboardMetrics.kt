package com.campusfix.domain.model

import java.util.concurrent.TimeUnit

/* ===================== HU10 - Dashboard de metricas y SLA ===================== */

/**
 * HU10 - Politica de SLA (Service Level Agreement).
 *
 * Define el tiempo objetivo de resolucion segun la urgencia del ticket.
 * Se usa para dos cosas:
 *  - Marcar como "vencido" un ticket activo cuya antiguedad supera el objetivo.
 *  - Calcular el % de cumplimiento sobre los tickets ya resueltos.
 *
 * Los umbrales son parametros de negocio; se centralizan aqui para poder
 * ajustarlos sin tocar la UI ni el ViewModel.
 */
object SlaPolicy {

    /** Tiempo objetivo de resolucion (en horas) por nivel de urgencia. */
    private val objetivoHoras: Map<Urgency, Long> = mapOf(
        Urgency.ALTA to 4L,
        Urgency.MEDIA to 24L,
        Urgency.BAJA to 72L,
    )

    /** Objetivo de resolucion en milisegundos para una urgencia dada. */
    fun objetivoMillis(urgencia: Urgency): Long =
        TimeUnit.HOURS.toMillis(objetivoHoras[urgencia] ?: 24L)

    /** Objetivo de resolucion en horas (para mostrarlo en la UI). */
    fun objetivoHoras(urgencia: Urgency): Long = objetivoHoras[urgencia] ?: 24L

    /** Un ticket esta "activo" si aun no fue resuelto ni cerrado. */
    fun esActivo(estado: TicketStatus): Boolean =
        estado == TicketStatus.ABIERTO ||
            estado == TicketStatus.ASIGNADO ||
            estado == TicketStatus.EN_ATENCION

    /**
     * Indica si un ticket ACTIVO esta vencido: sigue sin resolverse y ya paso
     * mas tiempo del objetivo de SLA para su urgencia.
     */
    fun estaVencido(ticket: Ticket, ahora: Long = System.currentTimeMillis()): Boolean {
        if (!esActivo(ticket.estado)) return false
        val transcurrido = ahora - ticket.creadoEn
        return transcurrido > objetivoMillis(ticket.urgencia)
    }

    /**
     * Indica si un ticket RESUELTO/CERRADO cumplio el SLA: el tiempo entre su
     * creacion y su resolucion no supero el objetivo para su urgencia.
     */
    fun cumplioSla(ticket: Ticket): Boolean {
        val resuelto = ticket.resueltoEn ?: return false
        return (resuelto - ticket.creadoEn) <= objetivoMillis(ticket.urgencia)
    }
}

/**
 * HU10 - Metricas agregadas que consume el dashboard del coordinador.
 * Es un objeto de solo lectura calculado a partir de la lista de tickets.
 */
data class DashboardMetrics(
    /** Tickets sin asignar (estado ABIERTO). Tarjeta "Abiertos". */
    val abiertos: Int = 0,
    /** Tickets activos que ya superaron su objetivo de SLA. Tarjeta "Vencidos". */
    val vencidos: Int = 0,
    /** Tickets en curso (ASIGNADO o EN_ATENCION). */
    val enProceso: Int = 0,
    /** Tickets resueltos o cerrados. */
    val resueltos: Int = 0,
    /** Total de tickets registrados. */
    val total: Int = 0,
    /** Conteo por cada estado, para el grafico de "Ticket por estado". */
    val conteoPorEstado: Map<TicketStatus, Int> = emptyMap(),
    /** Tiempo promedio de atencion (en minutos) sobre los tickets resueltos. */
    val tiempoPromedioMinutos: Int = 0,
    /** Porcentaje (0-100) de tickets resueltos que cumplieron el SLA. */
    val cumplimientoSlaPct: Int = 0,
    /** Tiempo promedio de atencion (minutos) por urgencia, para el grafico de barras. */
    val tiempoPromedioPorUrgencia: Map<Urgency, Int> = emptyMap(),
)
