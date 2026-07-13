package com.campusfix.ui.navigation

/** Rutas de navegacion de la app (Sprint 1). */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PROFILE = "profile"
    const val HOME = "home"
    // Pantalla para elegir el aula (escanear QR o buscarla en la lista)
    const val AULA_PICKER = "aula_picker"
    const val QR_SCAN = "qr_scan"
    // El reporte recibe el id del aula como argumento
    const val REPORT = "report/{aulaId}"
    fun report(aulaId: String) = "report/$aulaId"

    // HU06 - Asignacion de tickets (solo Coordinador)
    const val ASSIGNMENT = "assignment"
    const val TECH_MANAGEMENT = "tech_management"
    
    // HU06 - Tickets asignados (solo Tecnico)
    const val ASSIGNED_TICKETS = "assigned_tickets"
    // HU08 - Cierre del ticket con evidencia de solucion (solo Tecnico)
    const val TICKET_DETAIL = "ticket_detail/{ticketId}"
    fun ticketDetail(ticketId: String) = "ticket_detail/$ticketId"
}
