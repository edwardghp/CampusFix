package com.campusfix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
/** Base de datos SQLite local (Room). Offline-first para aulas. */
@Database(
    entities = [AulaEntity::class, TicketEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aulaDao(): AulaDao
    abstract fun ticketDao(): TicketDao
}
