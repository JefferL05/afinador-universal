package com.afinu.app.feature.tuner

import com.afinu.app.core.audio.NoteReading
import com.afinu.app.core.model.Instrument
import com.afinu.app.core.model.TuningPreset

/**
 * Estado completo da tela de afinador.
 *
 * Tudo que o Compose desenha vem daqui, e nada mais. Isso mantém a UI como
 * função pura do estado, que é o que permite testar a lógica sem instrumentar
 * tela.
 */
data class TunerUiState(
    /** `null` enquanto o ViewModel ainda não carregou as preferências. */
    val instrument: Instrument? = null,
    val tuning: TuningPreset? = null,
    val a4Hz: Float = 440f,
    val pitchOffsetCents: Float = 0f,

    /** Estado do microfone. */
    val micState: MicState = MicState.Idle,

    /** Leitura atual já estabilizada, ou null se nada foi detectado. */
    val reading: NoteReading? = null,

    /** Frequência suavizada, em Hz. Útil para mostrar o valor bruto. */
    val smoothedFrequencyHz: Float? = null,

    /** Índice (0-based) da corda que o app identificou, ou null em modo cromático. */
    val detectedStringIndex: Int? = null,

    /** A agulha deve piscar? Usado para sinalizar "acabou de entrar na nota". */
    val noteJustChanged: Boolean = false,

    /** Corda que o modo guiado está pedindo. */
    val guidedStringIndex: Int? = null,

    val confirmToneEnabled: Boolean = true,
    val confirmToneVolume: Float = 0.45f,
    val vibrationEnabled: Boolean = true,

    /** Contagem de afinações corretas na sessão, para a tela de treino. */
    val inTuneCount: Int = 0,
) {
    val isListening: Boolean get() = micState == MicState.Listening
    val hasPermission: Boolean get() = micState !is MicState.NeedsPermission

    /** Modo cromático: mostra todas as 12 notas em vez de só as cordas. */
    val isChromatic: Boolean
        get() = instrument?.family == com.afinu.app.core.model.InstrumentFamily.FREEFORM
}

sealed interface MicState {
    /** Microfone desligado. */
    data object Idle : MicState

    /** Falta a permissão de áudio. */
    data object NeedsPermission : MicState

    /** Capturando e processando. */
    data object Listening : MicState

    /** Permissão concedida mas o microfone não pôde ser aberto. */
    data class Failed(val reason: String) : MicState
}