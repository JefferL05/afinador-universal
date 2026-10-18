package com.afinu.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afinu.app.core.model.Instrument
import com.afinu.app.core.model.TuningPreset
import com.afinu.app.core.util.Capo
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.data.catalog.InstrumentCatalog
import com.afinu.app.data.prefs.AppSettings
import com.afinu.app.data.prefs.SettingsRepository
import com.afinu.app.data.repository.FavoriteTuning
import com.afinu.app.data.repository.FavoritesCodec
import com.afinu.app.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import javax.inject.Inject

/** Resposta a uma importação, mostrada como aviso temporário. */
sealed interface ImportOutcome {
    data class Success(val count: Int) : ImportOutcome
    data class Failure(val message: String) : ImportOutcome
    data object Idle : ImportOutcome
}

data class SettingsUiState(
    val instrument: Instrument = InstrumentCatalog.default,
    val tuning: TuningPreset = InstrumentCatalog.default.defaultTuning,
    val capoFret: Int = Capo.NONE,
    val a4Hz: Float = MusicTheory.DEFAULT_A4_HZ,
    val pitchOffsetCents: Float = 0f,
    val confirmToneEnabled: Boolean = true,
    val confirmToneVolume: Float = 0.45f,
    val vibrationEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val favorites: List<FavoriteTuning> = emptyList(),
    val importOutcome: ImportOutcome = ImportOutcome.Idle,
) {
    /** A4 com o deslocamento global aplicado — é o que o medidor realmente usa. */
    val effectiveA4Hz: Float
        get() = a4Hz * kotlin.math.exp(pitchOffsetCents / 1200f * LN_2)

    /** Frase curta que explica o efeito do deslocamento, para não ser mistério. */
    val pitchOffsetHint: String
        get() = when {
            pitchOffsetCents > 0.1f -> "+%.1f cents (mais agudo)".format(pitchOffsetCents)
            pitchOffsetCents < -0.1f -> "%.1f cents (mais grave)".format(pitchOffsetCents)
            else -> "0 cents (temperamento igual)"
        }

    private companion object {
        val LN_2 = kotlin.math.ln(2.0).toFloat()
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                favoritesRepository.favorites,
            ) { settings, favorites -> settings to favorites }
                .collect { (settings, favorites) ->
                    applySettings(settings, favorites)
                }
        }
    }

    private fun applySettings(s: AppSettings, favorites: List<FavoriteTuning>) {
        val instrument = InstrumentCatalog.find(s.instrumentId)
        val tuning = instrument.tunings.firstOrNull { it.id == s.tuningId }
            ?: instrument.defaultTuning
        _uiState.update {
            it.copy(
                instrument = instrument,
                tuning = tuning,
                capoFret = s.capoFret,
                a4Hz = s.a4Hz,
                pitchOffsetCents = s.pitchOffsetCents,
                confirmToneEnabled = s.confirmToneEnabled,
                confirmToneVolume = s.confirmToneVolume,
                vibrationEnabled = s.vibrationEnabled,
                keepScreenOn = s.keepScreenOn,
                favorites = favorites,
            )
        }
    }

    // --- Ações -----------------------------------------------------------------

    fun selectInstrument(id: String) = launch { settingsRepository.setInstrument(id) }

    /**
     * Trocar de instrumento também escolhe uma afinação válida para ele.
     *
     * Sem isso, usar "Drop D" num ukulele deixaria o app sem alvo e o medidor
     * mostraria um travessão para sempre.
     */
    fun selectTuning(id: String) = launch {
        val instrument = InstrumentCatalog.find(_uiState.value.instrument.id)
        val tuning = instrument.tunings.firstOrNull { it.id == id } ?: return@launch
        settingsRepository.setInstrument(instrument.id)
        settingsRepository.setTuning(tuning.id)
        // A afinação nova pode exigir uma janela diferente; zera o capo só se
        // ele ficar absurdo para o instrumento novo.
        if (!Capo.isPlausible(instrument, tuning, _uiState.value.capoFret)) {
            settingsRepository.setCapoFret(Capo.NONE)
        }
    }

    fun setCapoFret(fret: Int) = launch { settingsRepository.setCapoFret(Capo.clamp(fret)) }

    fun setA4(hz: Float) = launch { settingsRepository.setA4(hz) }

    fun setPitchOffset(cents: Float) = launch { settingsRepository.setPitchOffset(cents) }

    fun resetPitchOffset() = launch { settingsRepository.setPitchOffset(0f) }

    fun setConfirmToneEnabled(enabled: Boolean) =
        launch { settingsRepository.setConfirmToneEnabled(enabled) }

    fun setConfirmToneVolume(volume: Float) =
        launch { settingsRepository.setConfirmToneVolume(volume) }

    fun setVibrationEnabled(enabled: Boolean) =
        launch { settingsRepository.setVibrationEnabled(enabled) }

    fun setKeepScreenOn(enabled: Boolean) =
        launch { settingsRepository.setKeepScreenOn(enabled) }

    // --- Favoritos -------------------------------------------------------------

    fun saveCurrentAsFavorite(name: String) = launch {
        val state = _uiState.value
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@launch
        favoritesRepository.add(
            name = trimmed,
            instrumentId = state.instrument.id,
            tuningId = state.tuning.id,
            capoFret = state.capoFret,
        )
    }

    fun deleteFavorite(id: Long) = launch { favoritesRepository.delete(id) }

    /** Aplica um favorito: instrumento, afinação e capo de uma vez. */
    fun applyFavorite(favorite: FavoriteTuning) = launch {
        val instrument = favorite.instrument ?: return@launch
        val tuning = favorite.tuning ?: return@launch
        settingsRepository.setInstrument(instrument.id)
        settingsRepository.setTuning(tuning.id)
        settingsRepository.setCapoFret(favorite.capoFret)
    }

    // --- Importação e exportação ----------------------------------------------

    fun exportAsText(): String = FavoritesCodec.toExport(
        favorites = _uiState.value.favorites,
        appVersion = com.afinu.app.BuildConfig.VERSION_NAME,
        exportedAtEpochMs = System.currentTimeMillis(),
    )

    /**
     * Importa preservando os itens existentes.
     *
     * Importar não apaga o que já está salvo: o usuário que cola um arquivo
     * compartilhado por um colega espera ganhar as afinações dele, não perder
     * as suas.
     */
    fun importFromText(text: String) = launch {
        try {
            val items = FavoritesCodec.fromExport(text)
            if (items.isEmpty()) {
                _uiState.update { it.copy(importOutcome = ImportOutcome.Failure("empty")) }
                return@launch
            }
            items.forEach { item ->
                favoritesRepository.add(
                    name = item.name,
                    instrumentId = item.instrumentId,
                    tuningId = item.tuningId,
                    capoFret = item.capoFret,
                )
            }
            _uiState.update { it.copy(importOutcome = ImportOutcome.Success(items.size)) }
        } catch (e: SerializationException) {
            _uiState.update { it.copy(importOutcome = ImportOutcome.Failure(e.message ?: "invalid")) }
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(importOutcome = ImportOutcome.Failure("invalid")) }
        }
    }

    fun dismissImportOutcome() {
        _uiState.update { it.copy(importOutcome = ImportOutcome.Idle) }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}