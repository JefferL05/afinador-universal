package com.afinu.app.core.audio

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sin

/**
 * Trava a correcao do CMND (normalizacao desde tau = 1): nota aguda com ruido
 * pode ate desistir (null), mas nunca pode virar outra nota.
 */
class YinDetectorHighNoteTest {

    private val sampleRate = 48000

    private fun frame(f0: Double, noise: Double, seed: Long): FloatArray {
        val rnd = java.util.Random(seed)
        return FloatArray(YinDetector.WINDOW_STANDARD) { i ->
            val t = i / sampleRate.toDouble()
            val s = sin(2 * PI * f0 * t) +
                0.5 * sin(2 * PI * 2 * f0 * t) +
                0.3 * sin(2 * PI * 3 * f0 * t)
            (0.3 * s / 1.8 + noise * rnd.nextGaussian()).toFloat()
        }
    }

    private fun centsOff(measured: Float, expected: Double): Double =
        1200.0 * ln(measured / expected) / ln(2.0)

    @Test
    fun ruidoEmNotaAgudaNuncaViraNotaErrada() {
        val detector = YinDetector(sampleRate)
        for (f0 in doubleArrayOf(440.0, 659.26, 880.0, 1046.5)) {
            for (seed in 1L..20L) {
                val est = detector.detect(frame(f0, noise = 0.05, seed = seed)) ?: continue
                val erro = centsOff(est.frequencyHz, f0)
                assertTrue(
                    "f0=$f0 seed=$seed leu ${est.frequencyHz} Hz (${erro} cents)",
                    abs(erro) < 50.0,
                )
            }
        }
    }

    @Test
    fun sinalLimpoAgudoContinuaPreciso() {
        val detector = YinDetector(sampleRate)
        for (f0 in doubleArrayOf(440.0, 880.0, 1046.5)) {
            val est = detector.detect(frame(f0, noise = 0.0, seed = 1L))
            assertTrue("f0=$f0 nao detectado", est != null)
            assertTrue(abs(centsOff(est!!.frequencyHz, f0)) < 1.0)
        }
    }
}
