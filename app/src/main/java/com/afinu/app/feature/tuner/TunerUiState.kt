package com.afinu.app.feature.tuner

import com.afinu.app.core.audio.NoteReading
import com.afinu.app.core.model.Instrument
import com.afinu.app.core.model.InstrumentFamily
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
    /** Afinação a céu aberto, como está no catálogo. */
    val tuning: TuningPreset? = null,
    /** Afinação com o capo aplicado — é esta que o medidor usa como alvo. */
    val effectiveTuning: TuningPreset? = null,
    /** Casa do capo em uso, de 0 (sem capo) a 12. */
    val capoFret: Int = 0,
    val a4Hz: Float = 440f,
    val pitchOffsetCents: Float = 0f,

    /** Estado do microfone. */
    val micState: MicState = MicState.Idle,

    /**
     * Permissão de microfone **lida do sistema**, nunca deduzida de [micState].
     *
     * Antes era `micState !is NeedsPermission`, o que devolvia `true` para um
     * estado de erro. Com isso, quem tentava recuperar via "Tentar de novo"
     * enxergava "já tem permissão" e o afinador ficava preso no erro até o
     * usuário matar o app.
     */
    val hasPermission: Boolean = false,

    /** Leitura atual já estabilizada, ou null se nada foi detectado. */
    val reading: NoteReading? = null,

    /** Frequência suavizada, em Hz. Útil para mostrar o valor bruto. */
    val smoothedFrequencyHz: Float? = null,

    /** Índice (0-based) da corda que o app identificou, ou null em modo cromático. */
    val detectedStringIndex: Int? = null,

    /** A agulha deve piscar? Usado para sinalizar "acabou de entrar na nota". */
    val noteJustChanged: Boolean = false,

    /**
     * A nota entrou na zona "no ponto" neste quadro.
     *
     * É uma **transição**, não um estado: fica true por exatamente um quadro e
     * depois false. É o que dispara o bip e o anúncio do TalkBack uma única
     * vez, em vez de a cada quadro enquanto a nota estiver afinada.
     */
    val justGotInTune: Boolean = false,

    /** Corda que o modo guiado está pedindo. */
    val guidedStringIndex: Int? = null,

    val confirmToneEnabled: Boolean = true,
    val confirmToneVolume: Float = 0.45f,
    val vibrationEnabled: Boolean = true,

    /** Contagem de afinações corretas na sessão, para a tela de treino. */
    val inTuneCount: Int = 0,
) {
    val isListening: Boolean get() = micState == MicState.Listening

    /** O capo está em uso? Muda o que o cabeçalho e a tela de ajustes mostram. */
    val hasCapo: Boolean get() = capoFret != 0

    /** Rótulo do cabeçalho: afinação com o capo já embutido. */
    val tuningLabel: String?
        get() = effectiveTuning?.labelWithCapo(capoFret) ?: tuning?.label

    /**
     * Modo cromático: mostra todas as 12 notas em vez de só as cordas.
     *
     * Só faz sentido para instrumentos sem afinação fixa; um violão nunca entra
     * em cromático, mesmo que o usuário toque uma nota fora das cordas.
     */
    val isChromatic: Boolean
        get() = instrument?.family == InstrumentFamily.FREEFORM
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