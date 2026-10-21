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

    /**
     * O estado de cada corda, para o painel de cravelhas do modo violão.
     *
     * ## Por que o app precisa guardar isso
     * O microfone ouve uma corda por vez. Mas quem afina quer ver as seis ao
     * mesmo tempo: qual já está no ponto, qual precisa ser apertada, qual
     * ainda nem foi tocada. A leitura corrente é de uma corda só, e o resto
     * da informação vive aqui.
     *
     * A entrada de uma corda só muda quando há leitura para ela, então uma
     * corda que ninguém tocou fica sem entrada — que é o estado correto, e
     * não um bug.
     */
    val stringStatus: Map<Int, StringStatus> = emptyMap(),

    /**
     * Detectar a corda automaticamente?
     *
     * Desligado, o app afina a corda que o usuário escolheu, e não o que ele
     * tocar. É o que se quer quando há microfone ambiente tocando junto.
     */
    val autoDetect: Boolean = true,

    /**
     * Tocar as cordas em sequência, sem parar?
     *
     * O ciclo vai da 6ª à 1ª, que é a ordem em que se afina um instrumento
     * novo. Com o loop ligado, ele recomeça ao terminar.
     */
    val loopContinuo: Boolean = false,

    /** A sequência de referência está tocando agora? */
    val tocandoSequencia: Boolean = false,
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

/**
 * O estado de uma corda, como o painel de cravelhas mostra.
 *
 * @param midi a nota alvo da corda, com a afinação e o capo já aplicados.
 * @param cents o desvio medido na última vez que a corda foi ouvida.
 * @param frequencyHz a frequência medida naquela leitura.
 * @param inTune a corda estava dentro da zona de tolerância.
 */
data class StringStatus(
    val midi: Int,
    val cents: Float,
    val frequencyHz: Float,
    val inTune: Boolean,
)

sealed interface MicState {
    /** Microfone desligado. */
    data object Idle : MicState

    /** Falta a permissão de áudio. */
    data object NeedsPermission : MicState

    /** Capturando e processando. */
    data object Listening : MicState

    /** Permissão concedida mas o microfone não pôde ser aberto. */
    data class Failed(val causa: MicFailure) : MicState
}

/**
 * Motivo de [MicState.Failed] — motivo, e não texto.
 *
 * O ViewModel não tem `Context`, então guardava a frase pronta em português e
 * ela aparecia igual em inglês e em espanhol. A tela resolve o motivo para o
 * idioma certo.
 */
enum class MicFailure {
    /** A permissão existe, mas a captura não abriu — em geral outro app está com o microfone. */
    INDISPONIVEL,

    /** A captura estava rodando e parou: outro app tomou o microfone ou o sistema a derrubou. */
    INTERRUPTO,
}