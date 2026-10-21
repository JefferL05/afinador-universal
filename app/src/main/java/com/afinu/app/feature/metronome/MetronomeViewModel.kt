package com.afinu.app.feature.metronome

import android.os.SystemClock
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
    /** Quantos toques o "tap tempo" já registrou. */
    val tapCount: Int = 0,
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

    /** Os instantes dos toques do "tap tempo". */
    private var toquesTap = mutableListOf<Long>()

    /**
     * Quanto tempo um toque continua valendo para o "tap tempo".
     *
     * Dois segundos e meio: o suficiente para o intervalo de um tempo lento
     * (60 BPM), e curto o bastante para que uma batida solta muito depois não
     * contamine a média.
     */
    private companion object {
        const val JANELA_TAP_MS = 2_500L
    }
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

    /**
     * Marca o andamento no ritmo em que o usuário bate.
     *
     * ## Como funciona
     * Guardam-se os instantes dos toques. Com dois ou mais, o andamento é o
     * inverso do intervalo médio entre eles. Depois de três toques, o valor
     * deixa de oscilar — e é aí que ele fica confiável.
     *
     * Passados alguns segundos sem toque, a sequência é esquecida: uma
     * batida isolada muito depois da outra não é um andamento, e tratá-la
     * como se fosse zeraria o metrônomo no meio da música.
     */
    fun tapTempo() {
        val agora = SystemClock.elapsedRealtime()
        // Toques antigos demais não contam: uma batida isolada muito depois da
        // outra não é um andamento, e tratá-la como se fosse zeraria o
        // metrônomo no meio da música.
        val recentes = toquesTap.filter { agora - it <= JANELA_TAP_MS }
        val novos = (recentes + agora).toMutableList()
        toquesTap = novos

        _uiState.update { it.copy(tapCount = novos.size) }
        if (novos.size < 2) return

        val intervalos = novos.zipWithNext { a, b -> b - a }
        val medio = intervalos.sum() / intervalos.size
        if (medio <= 0L) return

        val bpm = (60_000.0 / medio).toFloat().coerceIn(
            MetronomeConfig.MIN_BPM.toFloat(),
            MetronomeConfig.MAX_BPM.toFloat(),
        )
        setBpm(bpm)
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