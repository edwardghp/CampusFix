package com.campusfix.data.local


import androidx.room.Entity
import androidx.room.PrimaryKey
import com.campusfix.domain.model.Aula
import com.campusfix.domain.model.FaultCategory
import com.campusfix.domain.model.Urgency
import com.campusfix.domain.model.TicketStatus
import androidx.room.TypeConverter
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.model.ChatMessage
/* ============ ENTIDADES ROOM (SQLite local - HU03) ============ */

@Entity(tableName = "aulas")
data class AulaEntity(
    @PrimaryKey val id: String,
    val codigo: String,
    val nombre: String,
    val facultad: String,
    val edificio: String,
    val qrCode: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
) {
    fun toDomain() = Aula(id, codigo, nombre, facultad, edificio, qrCode, latitud, longitud)
    companion object {
        fun from(a: Aula) = AulaEntity(a.id, a.codigo, a.nombre, a.facultad, a.edificio, a.qrCode, a.latitud, a.longitud)
    }
}

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    val aulaId: String,
    val aulaNombre: String,
    val aulaLat: Double? = null,
    val aulaLng: Double? = null,
    val categoria: FaultCategory,
    val urgencia: Urgency,
    val descripcion: String,
    /** Rutas locales de fotos pendientes de subir, separadas por '|' */
    val fotosLocales: String,
    /** Ruta local del audio pendiente de subir */
    val audioLocal: String,
    /** URLs remotas (Storage) una vez sincronizado, separadas por '|' */
    val fotoUrlsRemotas: String = "",
    val audioUrlRemoto: String = "",
    val reportanteUid: String,
    val tecnicoId: String? = null,
    val tecnicoNombre: String? = null,
    val fechaAsignacion: Long? = null,
    val estado: TicketStatus,
    val creadoEn: Long,
    val actualizadoEn: Long = creadoEn,
    val sincronizado: Boolean,
    // ---- HU08: base de conocimiento local de soluciones ----
    val solucionDescripcion: String = "",
    val solucionFotoUrl: String = "",
    val tiempoEmpleadoMinutos: Int? = null,
    val resueltoEn: Long? = null,
    val calificacion: Int? = null,
) {
    fun toDomain() = Ticket(
        id = id, aulaId = aulaId, aulaNombre = aulaNombre, aulaLat = aulaLat, aulaLng = aulaLng,
        categoria = categoria, urgencia = urgencia, descripcion = descripcion,
        fotoUrls = if (fotoUrlsRemotas.isBlank()) emptyList() else fotoUrlsRemotas.split("|"),
        audioUrl = audioUrlRemoto,
        reportanteUid = reportanteUid,
        tecnicoId = tecnicoId, tecnicoNombre = tecnicoNombre, fechaAsignacion = fechaAsignacion,
        estado = estado, creadoEn = creadoEn, sincronizado = sincronizado,
        solucionDescripcion = solucionDescripcion, solucionFotoUrl = solucionFotoUrl,
        tiempoEmpleadoMinutos = tiempoEmpleadoMinutos, resueltoEn = resueltoEn,
        calificacion = calificacion,
    )

    /** HU07 - Mapea un Ticket de dominio (p.ej. recibido por listener de Firestore) a su entidad local. */
    companion object {
        fun from(t: Ticket) = TicketEntity(
            id = t.id, aulaId = t.aulaId, aulaNombre = t.aulaNombre, aulaLat = t.aulaLat, aulaLng = t.aulaLng,
            categoria = t.categoria, urgencia = t.urgencia, descripcion = t.descripcion,
            fotosLocales = "", audioLocal = "",
            fotoUrlsRemotas = t.fotoUrls.joinToString("|"), audioUrlRemoto = t.audioUrl,
            reportanteUid = t.reportanteUid, tecnicoId = t.tecnicoId, tecnicoNombre = t.tecnicoNombre,
            fechaAsignacion = t.fechaAsignacion, estado = t.estado, creadoEn = t.creadoEn,
            actualizadoEn = t.actualizadoEn, sincronizado = t.sincronizado,
        )
    }
}

@Entity(
    tableName = "chat_messages",
    indices = [androidx.room.Index(value = ["ticketId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
) {
    fun toDomain() = ChatMessage(
        id = id.toString(),
        ticketId = ticketId,
        role = role,
        content = content,
        timestamp = timestamp
    )

    companion object {
        fun from(m: ChatMessage) = ChatMessageEntity(
            ticketId = m.ticketId,
            role = m.role,
            content = m.content,
            timestamp = m.timestamp
        )
    }
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
