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
