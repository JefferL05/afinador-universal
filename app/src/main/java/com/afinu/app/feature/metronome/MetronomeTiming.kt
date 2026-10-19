package com.afinu.app.feature.metronome

/**
 * Definições de um metrônomo.
 *
 * @param bpm batidas por minuto, de 30 a 300.
 * @param beatsPerBar tempos fortes por compasso (4/4 = 4, 7/8 = 7).
 * @param subdivisionsPerBeat divisões de cada tempo: 1 = semínima,
 *   2 = colcheia, 4 = semicolcheia.
 * @param accentEvery acentuar a cada N tempos. Acentuar todo tempo (1) é o
 *   padrão; 2 acentua a cada duas batidas, comum em estilos com swing.
 */
data class MetronomeConfig(
    val bpm: Float = 120f,
    val beatsPerBar: Int = 4,
    val subdivisionsPerBeat: Int = 1,
    val accentEvery: Int = 1,
) {
    init {
        require(bpm in MIN_BPM..MAX_BPM) { "BPM $bpm fora de $MIN_BPM..$MAX_BPM" }
        require(beatsPerBar in 1..16) { "tempos por compasso inválido: $beatsPerBar" }
        require(subdivisionsPerBeat in 1..8) { "subdivisões inválidas: $subdivisionsPerBeat" }
        require(accentEvery in 1..beatsPerBar) { "acento a cada $accentEvery tempos" }
    }

    /** Subdivisões por compasso. */
    val subdivisionsPerBar: Int get() = beatsPerBar * subdivisionsPerBeat

    companion object {
        const val MIN_BPM = 30f
        const val MAX_BPM = 300f

        /** 4/4, 3/4, 2/4, 5/8, 6/8, 7/8, 9/8. */
        val COMMON_METERS = listOf(4, 3, 2, 5, 6, 7, 9)

        val PRESETS = listOf(
            "Largo" to 50f,
            "Adagio" to 63f,
            "Andante" to 76f,
            "Moderato" to 100f,
            "Allegro" to 132f,
            "Presto" to 176f,
            "Prestissimo" to 220f,
        )
    }
}

/**
 * Matemática de tempo do metrônomo, em **amostras de áudio**.
 *
 * ## Por que contagem de amostras e não milissegundos
 * Um metrônomo que usa `Thread.sleep()` ou `System.currentTimeMillis()` erra a
 * cada batida, e o erro depende da carga da CPU: no meio de uma peça o
 * metrônimo "respira". O músico que toca junto com ele acelera sem perceber,
 * porque todo mundo se apoia na referência que o metrônomo dá.
 *
 * A solução é fazer do **relógio de áudio** o mestre: o `AudioTrack` consome
 * amostras a 48 000 Hz garantidos pelo cristal do aparelho. Se cada clique for
 * escrito exatamente N amostras depois do anterior, o espaçamento é exato por
 * construção.
 *
 * ## A armadilha do arredondamento, medida
 * Uma posição de tempo é quase sempre fracionária: a 127 BPM, um tempo mede
 * 22677,16 amostras. Só existem posições inteiras. As três formas de escolher:
 *
 * | Forma                                              | Erro em 10 min |
 * |----------------------------------------------------|----------------|
 * | Somar o inteiro arredondado a cada clique           | **262 ms**     |
 * | Andar em compassos inteiros (4 x 22677 = 90708)    | **262 ms**     |
 * | Posição do contador absoluto: `round(k × 22677,16)` | **0 ms**       |
 *
 * As duas primeiras derivam porque cada passo carrega um erro próprio, e o
 * erro se acumula. Só a terceira funciona: ela recalcula a posição a partir do
 * **contador de subdivisões desde o início**, arredondando uma única vez. O
 * erro fica limitado a meio amostra, para sempre.
 *
 * Foi por isso que [absoluteSubdivisionFrame] guarda um contador e não uma
 * posição — e por isso [MetronomeTimingTest] mede a deriva em vez de conferir
 * só a fórmula de uma batida.
 */
object MetronomeTiming {

    /**
     * Posição da subdivisão [subdivisionCounter] no fluxo de saída.
     *
     * [subdivisionCounter] é a contagem **global** desde o início do
     * metrônomo, não o índice dentro do compasso. Essa distinção é toda a
     * diferença entre um metrônomo que dura a noite inteira e um que puxa.
     */
    fun absoluteSubdivisionFrame(
        subdivisionCounter: Long,
        config: MetronomeConfig,
        sampleRate: Int,
    ): Long {
        val amostrasPorSubdivisao =
            sampleRate.toDouble() * 60.0 / (config.bpm * config.subdivisionsPerBeat)
        return Math.round(subdivisionCounter * amostrasPorSubdivisao)
    }

    /** Índice da subdivisão dentro do compasso atual. */
    fun localIndexInBar(subdivisionCounter: Long, config: MetronomeConfig): Int =
        (subdivisionCounter % config.subdivisionsPerBar).toInt()

    /** Índice do tempo dentro do compasso atual. */
    fun localBeatInBar(subdivisionCounter: Long, config: MetronomeConfig): Int =
        localIndexInBar(subdivisionCounter, config) / config.subdivisionsPerBeat

    /** Número de compassos já concluídos. Usado só para exibir na interface. */
    fun barNumber(subdivisionCounter: Long, config: MetronomeConfig): Long =
        subdivisionCounter / config.subdivisionsPerBar

    /**
     * A primeira subdivisão de cada tempo leva acento.
     *
     * O critério tem DUAS condições, e faltar a primeira é um erro sutil:
     *
     * 1. a subdivisão tem de ser a primeira do tempo — `localIndexInBar %
     *    subdivisoesPorTempo == 0`. Sem isso, com subdivisão 2 as colcheias
     *    levariam acento junto com as semínimas;
     * 2. o tempo só é acentuado a cada [MetronomeConfig.accentEvery] tempos.
     *    Com o valor padrão 1, todo tempo é acentuado.
     *
     * O critério é o do **tempo**, não "a cada 4 cliques": em 7/8 com subdivisão,
     * contar cliques erraria o compasso.
     */
    fun isAccent(subdivisionCounter: Long, config: MetronomeConfig): Boolean {
        val indice = localIndexInBar(subdivisionCounter, config)
        if (indice % config.subdivisionsPerBeat != 0) return false
        return localBeatInBar(subdivisionCounter, config) % config.accentEvery == 0
    }

    /** Amostras por tempo exatas, sem arredondar. Base de todo o cálculo. */
    fun exactFramesPerBeat(bpm: Float, sampleRate: Int): Double =
        sampleRate.toDouble() * 60.0 / bpm

    /**
     * Amostras por subdivisão arredondadas.
     *
     * Serve para exibição e para testes de sanidade, **não** para agendar
     * cliques — usar isto reintroduz a deriva descrita acima.
     */
    fun framesPerSubdivision(config: MetronomeConfig, sampleRate: Int): Long =
        Math.round(exactFramesPerBeat(config.bpm, sampleRate) / config.subdivisionsPerBeat)

    /** Duração de um clique em amostras. Curto o bastante para não cobrir a batida. */
    fun clickFrames(durationMs: Int, sampleRate: Int): Int =
        (sampleRate * durationMs / 1000).coerceAtLeast(1)

    /** Valor de BPM mais próximo de um passo do slider. */
    fun snapBpm(value: Float): Float = Math.round(value).toFloat().coerceIn(
        MetronomeConfig.MIN_BPM, MetronomeConfig.MAX_BPM
    )
}