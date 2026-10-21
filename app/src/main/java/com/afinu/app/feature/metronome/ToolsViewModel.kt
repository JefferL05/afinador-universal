package com.afinu.app.feature.metronome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afinu.app.core.audio.ConfirmTonePlayer
import com.afinu.app.core.audio.Waveform
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * O estado das ferramentas: o tom de referência.
 *
 * ## Por que isto é um ViewModel e não a própria tela
 * Porque tocar som é uma **efeito**, não um estado: o som sai do aparelho e
 * a tela só precisa saber o que foi pedido. O que a tela precisa guardar é o
 * que o usuário escolheu — a forma de onda, a nota — e isso é estado.
 */
@HiltViewModel
class ToolsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val tonePlayer: ConfirmTonePlayer,
) : ViewModel() {

    private val _waveform = MutableStateFlow(Waveform.SINE)
    val waveform: StateFlow<Waveform> = _waveform.asStateFlow()

    /** A4 de referência, em Hertz. */
    val a4Hz: StateFlow<Float> = settingsRepository.settings
        .map { it.a4Hz }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 440f,
        )

    /** O volume do tom de referência, o mesmo do bip de confirmação. */
    val volume: StateFlow<Float> = settingsRepository.settings
        .map { it.confirmToneVolume }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0.45f,
        )

    fun setWaveform(forma: Waveform) {
        _waveform.value = forma
    }

    /**
     * Toca uma nota, na altura do A4 do usuário.
     *
     * Com o afinador em 432 Hz, o Dó que o app mostra é o Dó de 432 — e é
     * esse que tem de soar.
     */
    fun tocarNota(midi: Int) {
        val hz = MusicTheory.frequencyOf(midi, a4Hz.value)
        if (!hz.isFinite() || hz <= 0f) return
        tonePlayer.play(
            frequencyHz = hz,
            durationMs = DURACAO_TOM_MS,
            volume = volume.value,
            waveform = _waveform.value,
        )
    }

    override fun onCleared() {
        tonePlayer.release()
        super.onCleared()
    }

    private companion object {
        /** 1,2 s: tempo de sobra para afinar contra a nota. */
        const val DURACAO_TOM_MS = 1_200
    }
}
