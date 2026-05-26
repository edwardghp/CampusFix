package com.campusfix.domain.model

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

data class User(
    val uid: String = "",
    val email: String = "",
    val nombre: String = "",
    val rol: UserRole = UserRole.ESTUDIANTE,
    val facultad: String = "",
    val cargo: String = "",
    val fotoUrl: String = "",
    val activo: Boolean = true,
)

enum class FaultCategory(val label: String) {
    PROYECTOR("Proyector"),
    RED("Red / Internet"),
    PC("Computadora"),
    CLIMATIZACION("Climatizacion"),
    OTRO("Otro"),
}

enum class Urgency(val label: String) {
    ALTA("Alta"), MEDIA("Media"), BAJA("Baja"),
}
enum class TicketStatus(val label: String) {
    ABIERTO("Abierto"),
    ASIGNADO("Asignado"),
    EN_ATENCION("En atencion"),
    RESUELTO("Resuelto"),
    CERRADO("Cerrado"),
}

data class Aula(
    val id: String = "",
    val codigo: String = "",
    val nombre: String = "",
    val facultad: String = "",
    val edificio: String = "",
    val qrCode: String = "",
)

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
    val estado: TicketStatus = TicketStatus.ABIERTO,
    val creadoEn: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false,
)
