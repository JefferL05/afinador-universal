package com.afinu.app.feature.chords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afinu.app.core.model.TuningPreset
import com.afinu.app.core.util.Capo
import com.afinu.app.core.util.ChordFinder
import com.afinu.app.core.util.ChordNames
import com.afinu.app.core.util.ChordQuality
import com.afinu.app.core.util.ChordShape
import com.afinu.app.core.util.ChordSpec
import com.afinu.app.core.util.FretPosition
import com.afinu.app.core.util.Fretboard
import com.afinu.app.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ChordsUiState(
    /** Afinação **efetiva**, já com o capo aplicado. É ela que gera os acordes. */
    val tuning: TuningPreset? = null,
    val capoFret: Int = Capo.NONE,
    val instrumentName: String = "",
    val rootSemitones: Int = 0,
    val quality: ChordQuality = ChordQuality.MAJOR,
    val shapes: List<ChordShape> = emptyList(),
    val searching: Boolean = false,
    /** Nota destacada no braço, para o fretboard. */
    val highlightSemitones: Int? = null,
    val fretPositions: List<FretPosition> = emptyList(),
) {
    val chordName: String get() = ChordNames.rootLabel(rootSemitones) + quality.suffix

    val hasShapes: Boolean get() = shapes.isNotEmpty()
}

/**
 * Acordes e braço do instrumento.
 *
 * ## Tudo nasce da afinação efetiva
 * Esta tela não guarda afinação própria. Ela lê a mesma que a Fase 4 produziu
 * (afinação do catálogo + capo) e busca os acordes a partir dela. É por isso
 * que trocar o capo aqui não é um "botão de transposição": o diagrama inteiro se
 * refaz para a nota que o músico realmente ouve.
 *
 * ## A busca roda fora da thread principal
 * [ChordFinder.findShapes] varre até 16 faixas de 5 casas em 6 cordas. Não é
 * instantâneo em aparelho de entrada, e travar a interface ao trocar de acorde
 * seria pior do que a busca ser assíncrona.
 */
@HiltViewModel
class ChordsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChordsUiState())
    val uiState: StateFlow<ChordsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { prefs ->
                val instrument = com.afinu.app.data.catalog.InstrumentCatalog.find(prefs.instrumentId)
                val tuning = instrument.tunings.firstOrNull { it.id == prefs.tuningId }
                    ?: instrument.defaultTuning
                val efetiva = Capo.effectiveOrUnshifted(tuning, prefs.capoFret)

                val mudou = _uiState.value.tuning?.id != efetiva.id
                _uiState.update {
                    it.copy(
                        tuning = efetiva,
                        capoFret = prefs.capoFret,
                        instrumentName = instrument.displayName,
                    )
                }
                if (mudou) buscar(_uiState.value.rootSemitones, _uiState.value.quality)
            }
        }
    }

    fun setRoot(semitones: Int) {
        _uiState.update { it.copy(rootSemitones = semitones) }
        buscar(semitones, _uiState.value.quality)
        destacar(semitones)
    }

    fun setQuality(quality: ChordQuality) {
        _uiState.update { it.copy(quality = quality) }
        buscar(_uiState.value.rootSemitones, quality)
    }

    fun clearHighlight() {
        _uiState.update { it.copy(highlightSemitones = null, fretPositions = emptyList()) }
    }

    /** Mostra no braço todas as posições de uma nota. */
    fun destacar(semitones: Int) {
        val tuning = _uiState.value.tuning ?: return
        val posicoes = with(Dispatchers.Default) {
            Fretboard.positionsForNoteName(tuning, ChordNames.rootLabel(semitones))
        }
        _uiState.update { it.copy(highlightSemitones = semitones, fretPositions = posicoes) }
    }

    private fun buscar(root: Int, quality: ChordQuality) {
        val tuning = _uiState.value.tuning ?: return
        _uiState.update { it.copy(searching = true) }

        // O braço já nasce marcando a RAIZ do acorde. Antes
        // `highlightSemitones` só mudava quando o usuário tocava numa nota, aí
        // a tela abria com uma grade de linhas completamente vazia — sem
        // nenhuma marca e sem explicação do que aquilo representava.
        destacar(root)

        viewModelScope.launch {
            val formas = withContext(Dispatchers.Default) {
                ChordFinder.findShapes(
                    tuning = tuning,
                    spec = ChordSpec(rootMidi = 60 + root, quality = quality),
                )
            }
            _uiState.update { it.copy(shapes = formas, searching = false) }
        }
    }
}