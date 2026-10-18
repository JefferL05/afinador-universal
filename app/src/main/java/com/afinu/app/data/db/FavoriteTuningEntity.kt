package com.afinu.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Uma afinação salva pelo usuário, com o capo que estava usado.
 *
 * Guarda os **identificadores** (instrumento + afinação + casa), não as notas.
 * Assim, corrigir um erro no catálogo de instrumentos corrige também os
 * favoritos já salvos pelo usuário — do contrário ele ficaria preso à versão
 * errada para sempre.
 */
@Entity(tableName = "favorite_tunings")
data class FavoriteTuningEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "instrument_id")
    val instrumentId: String,

    @ColumnInfo(name = "tuning_id")
    val tuningId: String,

    @ColumnInfo(name = "capo_fret")
    val capoFret: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long = 0,
)