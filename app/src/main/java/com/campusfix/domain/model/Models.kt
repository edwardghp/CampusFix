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
    // HU07 - Coordenadas para navegacion con Google Maps Intent
    val latitud: Double? = null,
    val longitud: Double? = null,
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
    // HU07 - Coordenadas del aula copiadas al crear el ticket (navegacion con Maps)
    val aulaLat: Double? = null,
    val aulaLng: Double? = null,
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
    val actualizadoEn: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false,
)
