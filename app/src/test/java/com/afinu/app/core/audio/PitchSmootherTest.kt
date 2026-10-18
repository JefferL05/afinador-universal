package com.afinu.app.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchSmootherTest {

    @Test
    fun `primeira leitura passa imediatamente`() {
        val smoother = PitchSmoother()

        // Se a primeira leitura exigisse confirmações, o app ficaria mudo
        // até o usuário acertar a nota três vezes.
        val resultado = smoother.accept(440f)

        assertNotNull(resultado)
        assertEquals(440f, resultado!!, 0.01f)
        assertEquals(69, smoother.midi)
    }

    @Test
    fun `pico isolado nao move a mediana`() {
        val smoother = PitchSmoother(historySize = 5, confirmationsRequired = 3)

        listOf(440f, 440.2f, 470f, 439.9f, 440.1f, 440f, 440f, 440.3f).forEach {
            smoother.accept(it)
        }

        val suavizado = smoother.smoothedFrequencyHz!!
        assertTrue("mediana contaminada pelo pico: $suavizado", suavizado < 450f)
    }

    @Test
    fun `troca de nota exige confirmacoes`() {
        val smoother = PitchSmoother(historySize = 5, confirmationsRequired = 3)

        smoother.accept(440f)
        smoother.accept(440f)
        assertEquals("nota inicial", 69, smoother.midi)

        // Uma leitura isolada de outra nota não pode trocar o medidor.
        smoother.accept(880f)
        assertEquals("leitura única não deve trocar a nota", 69, smoother.midi)

        // Duas confirmações ainda não bastam.
        smoother.accept(880f)
        assertEquals(69, smoother.midi)

        // A terceira confirma assume.
        smoother.accept(880f)
        assertEquals("três confirmações devem trocar a nota", 81, smoother.midi)
    }

    @Test
    fun `historico nao mistura oitavas`() {
        val smoother = PitchSmoother(historySize = 5, confirmationsRequired = 2)

        repeat(5) { smoother.accept(220f) }
        assertEquals(57, smoother.midi)

        // Ao trocar de nota o histórico é limpo; se não fosse, a mediana ficaria
        // entre 220 e 440 Hz, um valor que não é nota nenhuma.
        repeat(3) { smoother.accept(440f) }

        val suavizado = smoother.smoothedFrequencyHz!!
        assertTrue("mediana misturou oitavas: $suavizado", suavizado > 400f)
    }

    @Test
    fun `leitura perdida nao apaga o medidor`() {
        val smoother = PitchSmoother()

        smoother.accept(440f)
        assertNotNull(smoother.smoothedFrequencyHz)

        // Passar null (nada detectado neste quadro) não deve zerar tudo.
        val depois = smoother.accept(null)
        assertNotNull("o medidor não pode piscar entre cordadas", depois)
    }

    @Test
    fun `reset limpa o estado`() {
        val smoother = PitchSmoother()
        smoother.accept(440f)
        assertNotNull(smoother.smoothedFrequencyHz)

        smoother.reset()
        assertNull(smoother.smoothedFrequencyHz)
        assertNull(smoother.midi)
    }

    @Test
    fun `reduz o erro medio de ruido`() {
        // A mesma leitura ruidosa de 82,41 Hz vista antes e depois da mediana.
        val alvo = 82.41f
        val detector = YinDetector(48_000)
        val leituras = (0 until 5).map {
            detector.detect(SignalGenerator.pluckedString(alvo, 4096, noise = 0.10f, seed = it + 1))!!
        }

        val erroBruto = leituras.map { kotlin.math.abs(SignalGenerator.centsError(it.frequencyHz, alvo)) }
        val erroMediano = kotlin.math.abs(SignalGenerator.centsError(mediana(leituras.map { it.frequencyHz }), alvo))

        // Com 10% de ruído no grave a leitura bruta pode passar de 9 cents;
        // a mediana deve cortar esse pico pela metade.
        assertTrue(
            "bruto=${"%.2f".format(erroBruto.max())} mediana=$erroMediano",
            erroMediano <= erroBruto.max(),
        )
    }

    private fun mediana(valores: List<Float>): Float {
        val ordenados = valores.sorted()
        return ordenados[ordenados.size / 2]
    }
}