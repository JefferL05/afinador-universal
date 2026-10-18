package com.afinu.app.feature.metronome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetronomeUiState(
    val config: MetronomeConfig = MetronomeConfig(),
    val isRunning: Boolean = false,
    /** Tempo do compasso atual, de 1 a [MetronomeConfig.beatsPerBar]. */
    val accentBeat: Int = 1,
)

/**
 * Metrônomo.
 *
 * ## A configuração vive no ViewModel, não no motor
 * O [MetronomeEngine] sabe tocar cliques; ele não sabe o que é um compasso. A
 * separação deixa o motor testável sem UI e deixa a UI testável sem áudio.
 *
 * ## Trocar o BPM não reinicia a contagem
 * [MetronomeEngine.updateConfig] só troca os parâmetros. Se a contagem
 * reiniciasse a cada ajuste do slider, o músico perderia a referência de tempo
 * bem no momento em que mais precisa dela — a única hora que ele mexe no
 * andamento é no meio de uma frase.
 */
@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val engine: MetronomeEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetronomeUiState())
    val uiState: StateFlow<MetronomeUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    init {
        engine.onTick = { tick ->
            _uiState.update { it.copy(accentBeat = tick.barBeat) }
        }
    }

    fun toggle() {
        if (_uiState.value.isRunning) stop() else start()
    }

    private fun start() {
        val config = _uiState.value.config
        engine.start(config)
        _uiState.update { it.copy(isRunning = engine.isRunning, accentBeat = 1) }
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(250)
                if (!_uiState.value.isRunning) break
            }
        }
    }

    private fun stop() {
        engine.stop()
        tickJob?.cancel()
        tickJob = null
        _uiState.update { it.copy(isRunning = false, accentBeat = 1) }
    }

    fun setBpm(bpm: Float) {
        val config = _uiState.value.config.copy(
            bpm = MetronomeTiming.snapBpm(bpm),
        )
        aplicar(config)
    }

    fun setBeatsPerBar(beats: Int) = aplicar(_uiState.value.config.copy(beatsPerBar = beats))

    fun setSubdivisions(subdivisions: Int) =
        aplicar(_uiState.value.config.copy(subdivisionsPerBeat = subdivisions))

    fun setAccentEvery(every: Int) = aplicar(_uiState.value.config.copy(accentEvery = every))

    private fun aplicar(config: MetronomeConfig) {
        _uiState.update { it.copy(config = config) }
        if (_uiState.value.isRunning) engine.updateConfig(config)
    }

    override fun onCleared() {
        engine.stop()
        tickJob?.cancel()
        super.onCleared()
    }
}