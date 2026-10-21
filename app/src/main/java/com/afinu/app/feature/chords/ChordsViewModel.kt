package com.afinu.app.feature.chords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afinu.app.core.audio.ConfirmTonePlayer
import com.afinu.app.core.model.TuningPreset
import com.afinu.app.core.util.Capo
import com.afinu.app.core.util.ChordFinder
import com.afinu.app.core.util.ChordNames
import com.afinu.app.core.util.ChordQuality
import com.afinu.app.core.util.ChordShape
import com.afinu.app.core.util.ChordSpec
import com.afinu.app.core.util.ChordTone
import com.afinu.app.core.util.ChordTones
import com.afinu.app.core.util.FretPosition
import com.afinu.app.core.util.Fretboard
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.data.catalog.InstrumentCatalog
import com.afinu.app.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    /** A4 de referência do usuário, em Hz. As notas tocam na altura dele. */
    val a4Hz: Float = MusicTheory.DEFAULT_A4_HZ,
    /** Volume das notas, o mesmo do bip de confirmação. */
    val toneVolume: Float = 0.45f,
    /** As notas do acorde, na altura em que soam no instrumento. */
    val tones: List<ChordTone> = emptyList(),
    /** A nota que está soando agora, para o destaque. */
    val playingMidi: Int? = null,
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
    private val tonePlayer: ConfirmTonePlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChordsUiState())
    val uiState: StateFlow<ChordsUiState> = _uiState.asStateFlow()

    /** Job do destaque da nota que está soando. */
    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { prefs ->
                val instrument = InstrumentCatalog.find(prefs.instrumentId)
                val tuning = instrument.tunings.firstOrNull { it.id == prefs.tuningId }
                    ?: instrument.defaultTuning
                val efetiva = Capo.effectiveOrUnshifted(tuning, prefs.capoFret)

                val mudou = _uiState.value.tuning?.id != efetiva.id
                _uiState.update {
                    comNotas(
                        it.copy(
                            tuning = efetiva,
                            capoFret = prefs.capoFret,
                            instrumentName = instrument.displayName,
                            a4Hz = prefs.a4Hz,
                            toneVolume = prefs.confirmToneVolume,
                        ),
                    )
                }
                if (mudou) buscar(_uiState.value.rootSemitones, _uiState.value.quality)
            }
        }
    }

    fun setRoot(semitones: Int) {
        // A forma antiga não vale mais: as notas mostradas saem dela, e
        // mantê-las até a busca terminar mostraria as notas do acorde anterior
        // embaixo do nome do acorde novo.
        _uiState.update {
            comNotas(it.copy(rootSemitones = semitones, shapes = emptyList()))
        }
        buscar(semitones, _uiState.value.quality)
        destacar(semitones)
    }

    fun setQuality(quality: ChordQuality) {
        _uiState.update {
            comNotas(it.copy(quality = quality, shapes = emptyList()))
        }
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

    /**
     * Toca uma nota do acorde.
     *
     * ## Por que a frequência sai do A4 das preferências
     * Com o afinador em 432 Hz, o Dó que o app mostra é o Dó de 432. Tocar
     * 440 fixo seria o app se contradizendo com o resto de si mesmo — o
     * músico ouviria uma nota e leria outra.
     *
     * ## Por que o som não duplica
     * O player reusa a mesma track: cada toque substitui o anterior, como um
     * piano. Tocar duas vezes seguidas dá dois sons, um atrás do outro, e
     * nunca dois sons sobrepostos.
     */
    fun tocarNota(midi: Int) {
        val estado = _uiState.value
        val hz = MusicTheory.frequencyOf(midi, estado.a4Hz)
        if (!hz.isFinite() || hz <= 0f) return

        tonePlayer.play(
            frequencyHz = hz,
            durationMs = NOTA_DURACAO_MS,
            volume = estado.toneVolume,
        )

        _uiState.update { it.copy(playingMidi = midi) }
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            delay(NOTA_DURACAO_MS.toLong() - 100L)
            // Só limpa se ainda for a mesma nota: tocar outra no meio já trocou
            // o destaque, e o timer antigo não pode apagar o novo.
            _uiState.update {
                if (it.playingMidi == midi) it.copy(playingMidi = null) else it
            }
        }
    }

    /**
     * Recalcula as notas do acorde a partir do estado.
     *
     * Elas dependem de quatro coisas ao mesmo tempo: a tônica, a qualidade, a
     * forma encontrada e a afinação. Qualquer uma delas muda a lista — e a
     * forma só chega depois da busca, que é assíncrona.
     */
    private fun comNotas(estado: ChordsUiState): ChordsUiState = estado.copy(
        tones = ChordTones.of(
            rootSemitones = estado.rootSemitones,
            quality = estado.quality,
            shape = estado.shapes.firstOrNull(),
            tuning = estado.tuning,
        ),
    )

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
            _uiState.update { comNotas(it.copy(shapes = formas, searching = false)) }
        }
    }

    override fun onCleared() {
        playbackJob?.cancel()
        tonePlayer.release()
        super.onCleared()
    }

    private companion object {
        /**
         * 0,9 s: tempo de sobra para ouvir a nota e afinar contra ela, sem
         * deixar um tom soando no aparelho depois que o usuário já entendeu.
         */
        const val NOTA_DURACAO_MS = 900
    }
}
