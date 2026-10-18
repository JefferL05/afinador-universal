package com.afinu.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteTuningDao {

    /**
     * Os mais recentes primeiro. Fluxo: a lista da tela de ajustes se atualiza
     * sozinha quando o usuário salva ou apaga algo, sem recarregar a tela.
     */
    @Query("SELECT * FROM favorite_tunings ORDER BY created_at DESC")
    fun observeAll(): Flow<List<FavoriteTuningEntity>>

    @Query("SELECT * FROM favorite_tunings WHERE id = :id")
    suspend fun findById(id: Long): FavoriteTuningEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteTuningEntity): Long

    @Update
    suspend fun update(entity: FavoriteTuningEntity)

    @Delete
    suspend fun delete(entity: FavoriteTuningEntity)

    @Query("DELETE FROM favorite_tunings")
    suspend fun deleteAll()
}