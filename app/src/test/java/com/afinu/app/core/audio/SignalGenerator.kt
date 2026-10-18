package com.afinu.app.core.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Gera sinais de teste.
 *
 * Não usar `AudioRecord` nos testes: a caixa de som do CI é umUNKNOWN e não
 * determina a saída. Com sinais sintetizados, o teste é determinístico e roda
 * em qualquer máquina — inclusive sem hardware de áudio.
 */
object SignalGenerator {

    /** Senóide pura. */
    fun sine(frequencyHz: Float, size: Int, sampleRate: Int = 48_000, amplitude: Float = 0.6f): FloatArray =
        FloatArray(size) { i ->
            (amplitude * sin(2 * PI * frequencyHz * i / sampleRate)).toFloat()
        }

    /**
     * Dente de serra com a mesma periodicidade da senóide.
     *
     * Importante para os testes de oitava: a serra é **assimétrica**, então só
     * tem um vale na autocorrelação em tau e tau/2 não é um ponto forte. Se o
     * detector dobrar a frequência aqui, o bug está na correção de oitava.
     */
    fun sawtooth(frequencyHz: Float, size: Int, sampleRate: Int = 48_000, amplitude: Float = 0.6f): FloatArray {
        val period = sampleRate.toDouble() / frequencyHz
        return FloatArray(size) { i ->
            val phase = i / period
            (amplitude * 2 * (phase - Math.floor(phase + 0.5))).toFloat()
        }
    }

    /**
     * Onda quadrada de 50% de ciclo.
     *
     * É o pior caso para correção de oitava: por simetria ela também tem um
     * vale em tau/2, mas com a MESMA profundidade que em tau. Uma correção
     * ingênua que só olhasse "existe vale em tau/2" dobraria a leitura aqui.
     */
    fun square(frequencyHz: Float, size: Int, sampleRate: Int = 48_000, amplitude: Float = 0.6f): FloatArray {
        val period = sampleRate.toDouble() / frequencyHz
        return FloatArray(size) { i ->
            if (((i / period) % 1.0) < 0.5) amplitude else -amplitude
        }
    }

    /**
     * Espectro com peso de corda: fundamental dominante e harmônicos com
     * queda de energia, mais o ruído do ambiente.
     */
    fun pluckedString(
        frequencyHz: Float,
        size: Int,
        sampleRate: Int = 48_000,
        amplitude: Float = 0.6f,
        noise: Float = 0f,
        seed: Int = 42,
    ): FloatArray {
        val random = Random(seed)
        val harmonicWeights = doubleArrayOf(1.0, 0.55, 0.33, 0.22, 0.14)
        return FloatArray(size) { i ->
            val base = 2 * PI * frequencyHz * i / sampleRate
            var value = 0.0
            harmonicWeights.forEachIndexed { index, weight ->
                value += weight * sin(base * (index + 1))
            }
            val ruido = if (noise > 0f) (random.nextDouble() * 2 - 1) * noise else 0.0
            (value * amplitude + ruido).toFloat()
        }
    }

    /** Ruído branco puro. Não deve produzir leitura nenhuma. */
    fun whiteNoise(size: Int, amplitude: Float = 0.05f, seed: Int = 7): FloatArray {
        val random = Random(seed)
        return FloatArray(size) { i ->
            ((random.nextDouble() * 2 - 1) * amplitude).toFloat()
        }
    }

    /** Desvio em cents entre o que foi detectado e o que era o alvo. */
    fun centsError(detectedHz: Float, expectedHz: Float): Float =
        1200f * kotlin.math.ln(detectedHz / expectedHz) / kotlin.math.ln(2f)
}