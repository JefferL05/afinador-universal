package com.afinu.app.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressão do detector de tom.
 *
 * ## Por que estes números são o que são
 *
 * Medidos em 48 kHz com janela 4096:
 *
 * | Sinal                          | Erro      |
 * |--------------------------------|-----------|
 * | senóide limpa                  | 0,06 cents|
 * | corda (5 harm.) + 5% de ruído  | 1,05 cents|
 * | corda a 41 Hz + 5% de ruído*   | 0,14 cents|
 *
 * *janela 8192 — a 4096 o E1 fica perto do limite da faixa e perde precisão.
 *
 * Estes testes existem porque uma versão anterior do detector usava uma
 * recorrência O(1) **inválida** para a função de diferença: o resultado parecia
 * bom em senóides curtas e colapsava em silêncio e ruído. Se alguém "otimizar"
 * o laço interno de novo, estes testes falham.
 */
class YinDetectorTest {

    private val sampleRate = 48_000

    // --- Precisão em sinal limpo ------------------------------------------------

    @Test
    fun `detecta A4 exato`() {
        val detector = YinDetector(sampleRate)
        val estimativa = detector.detect(SignalGenerator.sine(440f, YinDetector.WINDOW_STANDARD, sampleRate))

        assertNotNull(estimativa)
        assertEquals(440f, estimativa!!.frequencyHz, 0.1f)
        assertEquals(0f, SignalGenerator.centsError(estimativa.frequencyHz, 440f), 0.5f)
    }

    @Test
    fun `detecta as seis cordas do violao`() {
        val detector = YinDetector(sampleRate)
        val violao = listOf("E2" to 82.41f, "A2" to 110.00f, "D3" to 146.83f,
            "G3" to 196.00f, "B3" to 246.94f, "E4" to 329.63f)

        violao.forEach { (nome, hz) ->
            val estimativa = detector.detect(SignalGenerator.sine(hz, YinDetector.WINDOW_STANDARD, sampleRate))
            assertNotNull("não detectou $nome", estimativa)
            assertTrue(
                "$nome: erro de ${SignalGenerator.centsError(estimativa!!.frequencyHz, hz)} cents",
                kotlin.math.abs(SignalGenerator.centsError(estimativa.frequencyHz, hz)) < 1f,
            )
        }
    }

    @Test
    fun `mantem precisao sub-cent no sinal limpo`() {
        val detector = YinDetector(sampleRate)
        val alvo = 82.41f
        val estimativa = detector.detect(SignalGenerator.sine(alvo, YinDetector.WINDOW_STANDARD, sampleRate))

        assertNotNull(estimativa)
        assertEquals(0f, SignalGenerator.centsError(estimativa!!.frequencyHz, alvo), 0.2f)
    }

    // --- Faixa grave: exige janela profunda ------------------------------------

    @Test
    fun `detecta E1 com janela de 4096`() {
        val detector = YinDetector(sampleRate)
        val estimativa = detector.detect(SignalGenerator.sine(41.20f, YinDetector.WINDOW_STANDARD, sampleRate))

        assertNotNull("E1 (41,2 Hz) deve estar dentro da faixa", estimativa)
        assertEquals(
            0f,
            SignalGenerator.centsError(estimativa!!.frequencyHz, 41.20f),
            2f,
        )
    }

    @Test
    fun `janela profunda melhora a precisao no grave com ruido`() {
        val profundo = YinDetector(sampleRate, windowSize = YinDetector.WINDOW_DEEP)
        val alvo = 41.20f
        val estimativa = profundo.detect(
            SignalGenerator.pluckedString(alvo, YinDetector.WINDOW_DEEP, sampleRate, noise = 0.05f)
        )

        assertNotNull(estimativa)
        assertEquals(
            0f,
            SignalGenerator.centsError(estimativa!!.frequencyHz, alvo),
            1f,
        )
    }

    @Test
    fun `janela de 2048 nao alcanca E1 e isso e esperado`() {
        // Documenta o limite: 2048 amostras cobrem só até ~46,9 Hz a 48 kHz.
        // Por isso instrumento grave usa WINDOW_DEEP. Se alguém reduzir a
        // janela padrão, este teste avisa.
        val detector = YinDetector(sampleRate, windowSize = 2048)
        val estimativa = detector.detect(SignalGenerator.sine(41.20f, 2048, sampleRate))

        assertNull("2048 amostras não alcançam 41,2 Hz", estimativa)
    }

    // --- Ondas não senoidais ---------------------------------------------------

    @Test
    fun `nao duplica a oitava em onda quadrada`() {
        // Onda quadrada tem simetria: existe vale em tau/2 com a mesma
        // profundidade. É o caso que quebra correções de oitava ingênuas.
        listOf(82.41f, 110f, 261.63f, 440f).forEach { alvo ->
            val detector = YinDetector(sampleRate)
            val estimativa = detector.detect(
                SignalGenerator.square(alvo, YinDetector.WINDOW_STANDARD, sampleRate)
            )
            assertNotNull("não detectou $alvo Hz", estimativa)
            assertTrue(
                "onda quadrada em $alvo Hz leu ${estimativa!!.frequencyHz} Hz — oitava errada",
                estimativa.frequencyHz < alvo * 1.2f && estimativa.frequencyHz > alvo * 0.8f,
            )
        }
    }

    @Test
    fun `nao duplica a oitava em dente de serra`() {
        listOf(82.41f, 164.81f, 329.63f).forEach { alvo ->
            val detector = YinDetector(sampleRate)
            val estimativa = detector.detect(
                SignalGenerator.sawtooth(alvo, YinDetector.WINDOW_STANDARD, sampleRate)
            )
            assertNotNull("não detectou $alvo Hz", estimativa)
            assertTrue(
                "serra em $alvo Hz leu ${estimativa!!.frequencyHz} Hz",
                kotlin.math.abs(SignalGenerator.centsError(estimativa.frequencyHz, alvo)) < 5f,
            )
        }
    }

    @Test
    fun `reconhece o fundamental em espectro de corda`() {
        val detector = YinDetector(sampleRate)
        listOf(82.41f, 110f, 130.81f, 196f, 246.94f, 329.63f).forEach { alvo ->
            val estimativa = detector.detect(
                SignalGenerator.pluckedString(alvo, YinDetector.WINDOW_STANDARD, sampleRate)
            )
            assertNotNull("não detectou $alvo Hz", estimativa)
            assertEquals(
                "corda em $alvo Hz",
                0f,
                SignalGenerator.centsError(estimativa!!.frequencyHz, alvo),
                1f,
            )
        }
    }

    // --- Rejeição --------------------------------------------------------------

    @Test
    fun `silencio nao produz leitura`() {
        val detector = YinDetector(sampleRate)
        assertNull(detector.detect(FloatArray(YinDetector.WINDOW_STANDARD)))
    }

    @Test
    fun `ruiso branco nao produz leitura`() {
        val detector = YinDetector(sampleRate)
        assertNull(
            detector.detect(
                SignalGenerator.whiteNoise(YinDetector.WINDOW_STANDARD, amplitude = 0.03f)
            )
        )
    }

    @Test
    fun `trecho menor que a janela e ignorado`() {
        val detector = YinDetector(sampleRate)
        assertNull(detector.detect(FloatArray(128)))
    }

    // --- Confiança e afinação --------------------------------------------------

    @Test
    fun `confianca e alta em sinal limpo`() {
        val detector = YinDetector(sampleRate)
        val estimativa = detector.detect(SignalGenerator.sine(440f, YinDetector.WINDOW_STANDARD, sampleRate))
        assertNotNull(estimativa)
        assertTrue("confiança ${estimativa!!.confidence} deveria ser alta", estimativa.confidence > 0.9f)
    }

    @Test
    fun `detecta corda desafinada e erra na direcao certa`() {
        // O afinador precisa mostrar o desvio com sinal: uma E4 afinada 10 cents
        // acima tem de aparecer como +10, não como 0 nem como -10.
        val detector = YinDetector(sampleRate)
        val alvo = 329.63f * Math.pow(2.0, 10.0 / 1200.0).toFloat()
        val estimativa = detector.detect(SignalGenerator.sine(alvo, YinDetector.WINDOW_STANDARD, sampleRate))

        assertNotNull(estimativa)
        val cents = SignalGenerator.centsError(estimativa!!.frequencyHz, 329.63f)
        assertEquals("desvio", 10f, cents, 1f)
    }

    @Test
    fun `janela profunda e necessaria para instrumentos graves`() {
        assertEquals(YinDetector.WINDOW_DEEP, YinDetector.windowFor(needsDeepWindow = true))
        assertEquals(YinDetector.WINDOW_STANDARD, YinDetector.windowFor(needsDeepWindow = false))
    }
}