package com.afinu.app.core.audio

import com.afinu.app.core.util.MusicTheory
import kotlin.math.ceil

/**
 * Filtro que estabiliza a leitura do tom.
 *
 * Faz duas coisas distintas, e ambas são necessárias:
 *
 * 1. **Mediana móvel** sobre as frequências. Remove picos isolados sem
 *    introduzir o atraso de uma média (que é o que faz a agulha "flutuar").
 *
 * 2. **Histerese na troca de nota.** Uma nota nova só passa a valer depois de
 *    [confirmationsRequired] leituras consecutivas. Sem isso o medidor pisca
 *    entre duas notas quando o timbre muda, exatamente na transição entre
 *    cordas — que é quando o usuário mais precisa ler.
 *
 * Ao trocar de nota o histórico é descartado: misturar frequências de oitavas
 * diferentes numa mediana produziria um valor sem sentido musical.
 *
 * A instância NÃO é segura para uso concorrente.
 */
class PitchSmoother(
    private val historySize: Int = DEFAULT_HISTORY_SIZE,
    private val confirmationsRequired: Int = DEFAULT_CONFIRMATIONS,
) {
    private val history = ArrayDeque<Double>(historySize)
    private var stableMidi: Int? = null
    private var candidateMidi: Int? = null
    private var candidateCount = 0

    /** Frequência suavizada atual, ou null se ainda não há leitura alguma. */
    val smoothedFrequencyHz: Float?
        get() = if (history.isEmpty()) null else median().toFloat()

    /** Nota MIDI estável atual, ou null. */
    val midi: Int? get() = stableMidi

    fun reset() {
        history.clear()
        stableMidi = null
        candidateMidi = null
        candidateCount = 0
    }

    /**
     * Alimenta uma leitura. Passar `null` (nenhum tom detectado) não zera o
     * medidor: uma leitura perdida entre duas cordadas não deve apagar a
     * informação que ainda está válida.
     *
     * @return a frequência suavizada, ou null se nunca houve leitura.
     */
    fun accept(frequencyHz: Float?, a4Hz: Float = MusicTheory.DEFAULT_A4_HZ): Float? {
        if (frequencyHz == null || !frequencyHz.isFinite() || frequencyHz <= 0f) {
            candidateMidi = null
            candidateCount = 0
            return smoothedFrequencyHz
        }

        val midi = MusicTheory.nearestMidi(frequencyHz, a4Hz)

        if (midi == candidateMidi) {
            candidateCount++
        } else {
            candidateMidi = midi
            candidateCount = 1
        }

        when {
            // Primeira leitura do ciclo: já valida, senão o app ficaria mudo
            // até o usuário acertar a nota três vezes.
            stableMidi == null -> {
                stableMidi = midi
                history.clear()
            }

            midi != stableMidi && candidateCount >= confirmationsRequired -> {
                stableMidi = midi
                history.clear()
            }
        }

        // Só entra no histórico a leitura que pertence à nota estável. Assim a
        // mediana nunca fica contaminada por uma transição ainda não confirmada.
        if (midi == stableMidi) {
            if (history.size >= historySize) history.removeFirst()
            history.addLast(frequencyHz.toDouble())
        }

        return smoothedFrequencyHz
    }

    private fun median(): Double {
        val ordenadas = history.sorted()
        val n = ordenadas.size
        return if (n % 2 == 1) ordenadas[n / 2]
        else (ordenadas[n / 2 - 1] + ordenadas[n / 2]) / 2.0
    }

    companion object {
        /** Janela da mediana. Ímpar para não precisar tratar par. */
        const val DEFAULT_HISTORY_SIZE = 5

        /** Leituras consecutivas necessárias para trocar de nota. */
        const val DEFAULT_CONFIRMATIONS = 3

        /** Multiplicador de tamanho de buffer para a janela padrão. */
        fun requiredAudioBuffer(windowSize: Int, sampleRate: Int): Int =
            ceil(windowSize * 1.5).toInt()
    }
}