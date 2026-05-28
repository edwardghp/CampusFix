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
