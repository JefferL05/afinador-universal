package com.afinu.app.data.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Banco local do app.
 *
 * `fallbackToDestructiveMigration` é aceitável aqui **apenas** porque tudo o
 * que se guarda é recriável pelo usuário em segundos (afinações favoritas). Se
 * um dia houver dado insubstituível, esta linha tem que sair e vir uma migração
 * de verdade.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideFavoriteTuningDao(db: AppDatabase): FavoriteTuningDao = db.favoriteTunings()
}