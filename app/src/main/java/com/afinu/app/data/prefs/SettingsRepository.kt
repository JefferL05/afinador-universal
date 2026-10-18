package com.afinu.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.afinu.app.core.util.MusicTheory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "afinador_prefs")

/** Configurações do usuário que sobrevivem ao fechamento do app. */
data class AppSettings(
    val instrumentId: String,
    val tuningId: String,
    /** A4 de referência, de 415 a 466 Hz. */
    val a4Hz: Float,
    /** Deslocamento global do referencial, em cents. */
    val pitchOffsetCents: Float,
    val confirmToneEnabled: Boolean,
    val confirmToneVolume: Float,
    val vibrationEnabled: Boolean,
    val keepScreenOn: Boolean,
)

/**
 * Preferências persistidas.
 *
 * Tudo aqui é fluxo frio do DataStore — o ViewModel coleta, nunca bloqueia.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val INSTRUMENT = stringPreferencesKey("instrument_id")
        val TUNING = stringPreferencesKey("tuning_id")
        val A4 = floatPreferencesKey("a4_hz")
        val OFFSET = floatPreferencesKey("pitch_offset_cents")
        val TONE_ON = booleanPreferencesKey("confirm_tone_enabled")
        val TONE_VOL = floatPreferencesKey("confirm_tone_volume")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            instrumentId = prefs[Keys.INSTRUMENT] ?: "violao",
            tuningId = prefs[Keys.TUNING] ?: "padrao",
            a4Hz = (prefs[Keys.A4] ?: MusicTheory.DEFAULT_A4_HZ)
                .coerceIn(MusicTheory.MIN_A4_HZ, MusicTheory.MAX_A4_HZ),
            pitchOffsetCents = (prefs[Keys.OFFSET] ?: 0f).coerceIn(-50f, 50f),
            confirmToneEnabled = prefs[Keys.TONE_ON] ?: true,
            confirmToneVolume = (prefs[Keys.TONE_VOL] ?: 0.45f).coerceIn(0f, 1f),
            vibrationEnabled = prefs[Keys.VIBRATION] ?: true,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
        )
    }

    suspend fun setInstrument(id: String) = context.dataStore.edit { it[Keys.INSTRUMENT] = id }

    suspend fun setTuning(id: String) = context.dataStore.edit { it[Keys.TUNING] = id }

    suspend fun setA4(hz: Float) = context.dataStore.edit {
        it[Keys.A4] = hz.coerceIn(MusicTheory.MIN_A4_HZ, MusicTheory.MAX_A4_HZ)
    }

    suspend fun setPitchOffset(cents: Float) = context.dataStore.edit {
        it[Keys.OFFSET] = cents.coerceIn(-50f, 50f)
    }

    suspend fun setConfirmToneEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.TONE_ON] = enabled }

    suspend fun setConfirmToneVolume(volume: Float) =
        context.dataStore.edit { it[Keys.TONE_VOL] = volume.coerceIn(0f, 1f) }

    suspend fun setVibrationEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.VIBRATION] = enabled }

    suspend fun setKeepScreenOn(enabled: Boolean) =
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
}