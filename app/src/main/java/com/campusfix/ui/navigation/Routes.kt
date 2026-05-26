package com.campusfix.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PROFILE = "profile"
    const val HOME = "home"
    const val QR_SCAN = "qr_scan"
    // El reporte recibe el id del aula escaneada como argumento
    const val REPORT = "report/{aulaId}"
    fun report(aulaId: String) = "report/$aulaId"
}
