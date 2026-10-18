package com.afinu.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteTuningEntity::class],
    version = 1,
    // Sem export de schema até existir migração; ligar isso com um schema
    // versionado agora só geraria um diretório que ninguém usa.
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteTunings(): FavoriteTuningDao

    companion object {
        const val NAME = "afinador.db"
    }
}