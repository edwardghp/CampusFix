package com.campusfix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.campusfix.domain.model.Aula
import com.campusfix.domain.model.FaultCategory
import com.campusfix.domain.model.Urgency
import com.campusfix.domain.model.TicketStatus
import androidx.room.TypeConverter
import com.campusfix.domain.model.Ticket
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

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    val aulaId: String,
    val aulaNombre: String,
    val categoria: FaultCategory,
    val urgencia: Urgency,
    val descripcion: String,
    /** Rutas locales de fotos pendientes de subir, separadas por '|' */
    val fotosLocales: String,
    /** Ruta local del audio pendiente de subir */
    val audioLocal: String,
    val reportanteUid: String,
    val estado: TicketStatus,
    val creadoEn: Long,
    val sincronizado: Boolean,
) {
    fun toDomain() = Ticket(
        id = id, aulaId = aulaId, aulaNombre = aulaNombre, categoria = categoria,
        urgencia = urgencia, descripcion = descripcion, reportanteUid = reportanteUid,
        estado = estado, creadoEn = creadoEn, sincronizado = sincronizado,
    )
}

/** Convertidores para que Room pueda guardar los enums. */
class Converters {
    @TypeConverter fun catToString(v: FaultCategory) = v.name
    @TypeConverter fun stringToCat(v: String) = FaultCategory.valueOf(v)
    @TypeConverter fun urgToString(v: Urgency) = v.name
    @TypeConverter fun stringToUrg(v: String) = Urgency.valueOf(v)
    @TypeConverter fun statusToString(v: TicketStatus) = v.name
    @TypeConverter fun stringToStatus(v: String) = TicketStatus.valueOf(v)
}
