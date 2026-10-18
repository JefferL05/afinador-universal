package com.afinu.app.core.audio

/** Resultado de uma detecção de tom. */
data class PitchEstimate(
    /** Frequência fundamental estimada, em Hz. */
    val frequencyHz: Float,
    /** Confiança de 0 a 1. Valores baixos significam "não confie nesta leitura". */
    val confidence: Float,
)

/**
 * Contrato de um detector de frequência fundamental.
 *
 * Implementações NÃO devem alocar memória por chamada: [detect] roda dezenas de
 * vezes por segundo no caminho quente do laço de áudio.
 */
interface PitchDetector {
    val minFrequencyHz: Float
    val maxFrequencyHz: Float

    /** Retorna `null` se o trecho não contém um tom confiável (silêncio ou ruído). */
    fun detect(samples: FloatArray): PitchEstimate?
}