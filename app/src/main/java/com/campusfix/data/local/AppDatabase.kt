package com.campusfix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** Base de datos SQLite local (Room). Offline-first para aulas. */
@Database(
    entities = [AulaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aulaDao(): AulaDao
}
