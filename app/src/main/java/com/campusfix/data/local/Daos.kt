package com.campusfix.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/* ============ DAOs - acceso a la base de datos local ============ */

@Dao
interface AulaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(aulas: List<AulaEntity>)

    @Query("SELECT * FROM aulas ORDER BY codigo")
    fun observeAll(): Flow<List<AulaEntity>>

    @Query("SELECT * FROM aulas WHERE qrCode = :qr LIMIT 1")
    suspend fun findByQr(qr: String): AulaEntity?

    @Query("SELECT * FROM aulas WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): AulaEntity?

    @Query("SELECT COUNT(*) FROM aulas")
    suspend fun count(): Int
}

@Dao
interface TicketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ticket: TicketEntity)

    @Update
    suspend fun update(ticket: TicketEntity)

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE reportanteUid = :uid ORDER BY creadoEn DESC")
    fun observeByUser(uid: String): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE tecnicoId = :uid ORDER BY creadoEn DESC")
    fun observeByTechnician(uid: String): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE sincronizado = 0")
    suspend fun pendientes(): List<TicketEntity>
}
