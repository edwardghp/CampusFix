package com.campusfix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.campusfix.domain.model.Aula

/* ============ ENTIDADES ROOM (SQLite local - HU03) ============ */

@Entity(tableName = "aulas")
data class AulaEntity(
    @PrimaryKey val id: String,
    val codigo: String,
    val nombre: String,
    val facultad: String,
    val edificio: String,
    val qrCode: String,
) {
    fun toDomain() = Aula(id, codigo, nombre, facultad, edificio, qrCode)
    companion object {
        fun from(a: Aula) = AulaEntity(a.id, a.codigo, a.nombre, a.facultad, a.edificio, a.qrCode)
    }
}
