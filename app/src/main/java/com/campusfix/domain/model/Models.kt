package com.campusfix.domain.model

/* ===================== MODELOS DE DOMINIO (Sprint 1) ===================== */

/** Roles de usuario de CampusFix. */
enum class UserRole(val label: String) {
    DOCENTE("Docente"),
    ESTUDIANTE("Estudiante"),
    TECNICO("Tecnico de Soporte"),
    COORDINADOR("Coordinador de Soporte");

    companion object {
        fun fromName(name: String?): UserRole =
            entries.firstOrNull { it.name == name } ?: ESTUDIANTE
    }
}

/** Perfil del usuario (HU01 / HU02). */
data class User(
    val uid: String = "",
    val email: String = "",
    val nombre: String = "",
    val rol: UserRole = UserRole.ESTUDIANTE,
    val facultad: String = "",
    val cargo: String = "",
    val fotoUrl: String = "",
    val activo: Boolean = true,
    val especialidad: FaultCategory? = null,
    val fcmToken: String = "",
)

/** Aula de la facultad (HU03). El catalogo se guarda offline en Room. */
data class Aula(
    val id: String = "",
    val codigo: String = "",
    val nombre: String = "",
    val facultad: String = "",
    val edificio: String = "",
    val qrCode: String = "",
)

/** Categoria de la falla reportada (HU03). */
enum class FaultCategory(val label: String) {
    PROYECTOR("Proyector"),
    RED("Red / Internet"),
    PC("Computadora"),
    CLIMATIZACION("Climatizacion"),
    OTRO("Otro"),
}

/** Nivel de urgencia del ticket. En el Sprint 2 la IA lo sugiere automaticamente. */
enum class Urgency(val label: String) {
    ALTA("Alta"), MEDIA("Media"), BAJA("Baja"),
}

/** Estado del ticket (el ciclo completo se gestiona en Sprints 2 y 3). */
enum class TicketStatus(val label: String) {
    ABIERTO("Abierto"),
    ASIGNADO("Asignado"),
    EN_ATENCION("En atencion"),
    RESUELTO("Resuelto"),
    CERRADO("Cerrado"),
}

/** Ticket de incidencia (HU04). */
data class Ticket(
    val id: String = "",
    val aulaId: String = "",
    val aulaNombre: String = "",
    val categoria: FaultCategory = FaultCategory.OTRO,
    val urgencia: Urgency = Urgency.MEDIA,
    val descripcion: String = "",
    val fotoUrls: List<String> = emptyList(),
    val audioUrl: String = "",
    val reportanteUid: String = "",
    val tecnicoId: String? = null,
    val tecnicoNombre: String? = null,
    val fechaAsignacion: Long? = null,
    val estado: TicketStatus = TicketStatus.ABIERTO,
    val creadoEn: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false,

    // ---- HU08: cierre del ticket con evidencia de solucion ----
    /** Descripcion de la solucion aplicada, escrita por el tecnico. */
    val solucionDescripcion: String = "",
    /** URL en Firebase Storage de la foto del equipo reparado. */
    val solucionFotoUrl: String = "",
    /** Tiempo que le tomo al tecnico resolver la falla, en minutos. */
    val tiempoEmpleadoMinutos: Int? = null,
    /** Momento en que el tecnico marco el ticket como resuelto. */
    val resueltoEn: Long? = null,
    /** Calificacion de 1 a 5 estrellas que da el reportante al cerrar el ticket. */
    val calificacion: Int? = null,
)
