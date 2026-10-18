package com.afinu.app.feature.metronome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A propriedade central do metrônomo.
 *
 * Um metrônomo que "parece funcionar" e vai arrastando é pior do que nenhum:
 * o músico acelera junto sem perceber, porque todo mundo toca apoiado na
 * referência que o metrônomo dá.
 *
 * Estes testes medem a **deriva de verdade** — simulando 10 minutos de música e
 * comparando cada clique com a grade ideal — em vez de conferir apenas a
 * fórmula de uma batida.
 */
class MetronomeTimingTest {

    private val sr = 48_000

    // --- Valores básicos -------------------------------------------------------

    @Test
    fun `120 BPM sao duas batidas por segundo`() {
        // 120 BPM = 0,5 s entre batidas = 24000 amostras a 48 kHz.
        val config = MetronomeConfig(bpm = 120f, subdivisionsPerBeat = 1)
        assertEquals(24_000L, MetronomeTiming.absoluteSubdivisionFrame(1, config, sr))
        assertEquals(48_000L, MetronomeTiming.absoluteSubdivisionFrame(2, config, sr))
    }

    @Test
    fun `60 BPM e uma batida por segundo`() {
        val config = MetronomeConfig(bpm = 60f)
        assertEquals(48_000L, MetronomeTiming.absoluteSubdivisionFrame(1, config, sr))
    }

    @Test
    fun `o primeiro clique e na amostra zero`() {
        val config = MetronomeConfig()
        assertEquals(0L, MetronomeTiming.absoluteSubdivisionFrame(0, config, sr))
    }

    @Test
    fun `colcheia a 120 BPM e meia batida`() {
        val config = MetronomeConfig(bpm = 120f, subdivisionsPerBeat = 2)
        assertEquals(12_000L, MetronomeTiming.absoluteSubdivisionFrame(1, config, sr))
        assertEquals(24_000L, MetronomeTiming.absoluteSubdivisionFrame(2, config, sr))
    }

    // --- A propriedade que importa: a deriva é zero ---------------------------

    @Test
    fun `nao ha deriva em dez minutos a 127 BPM`() {
        // 127 é de propósito: 48000*60/127 = 22677,16 amostras por batida. A
        // fração 0,16 é exatamente o erro que se acumularia somando inteiros.
        assertDerivaMenorQue(
            config = MetronomeConfig(bpm = 127f, beatsPerBar = 4),
            minutos = 10,
            tolerancia = 0.5,
        )
    }

    @Test
    fun `nao ha deriva em sete oito com subdivisao tripla`() {
        // 7/8 com subdivisão 3 é o caso mais duro: 21 cliques por compasso,
        // fração fracionária em cada passo.
        assertDerivaMenorQue(
            config = MetronomeConfig(bpm = 143f, beatsPerBar = 7, subdivisionsPerBeat = 3),
            minutos = 10,
            tolerancia = 0.5,
        )
    }

    @Test
    fun `nao ha deriva em varios outros tempos`() {
        listOf(30f, 63f, 93.5f, 120f, 176f, 231f, 300f).forEach { bpm ->
            assertDerivaMenorQue(
                config = MetronomeConfig(bpm = bpm, beatsPerBar = 7, subdivisionsPerBeat = 4),
                minutos = 5,
                tolerancia = 0.5,
            )
        }
    }

    @Test
    fun `o espaçamento entre dois cliques consecutivos e sempre o mesmo`() {
        val config = MetronomeConfig(bpm = 137f, subdivisionsPerBeat = 3)
        val amostrasPorSubdivisao = MetronomeTiming
            .exactFramesPerBeat(config.bpm, sr) / config.subdivisionsPerBeat

        var anterior = MetronomeTiming.absoluteSubdivisionFrame(0, config, sr)
        var piorDesvio = 0.0
        for (k in 1L..2000L) {
            val atual = MetronomeTiming.absoluteSubdivisionFrame(k, config, sr)
            val esperado = k * amostrasPorSubdivisao
            piorDesvio = kotlin.math.max(
                piorDesvio,
                kotlin.math.abs((atual - anterior) - amostrasPorSubdivisao),
            )
            anterior = atual
        }
        // Numa grade correta, o desvio de um passo para o outro nunca passa de
        // 1 amostra, porque cada posição é arredondada de forma independente.
        assertTrue("desvio por passo de $piorDesvio amostras", piorDesvio <= 1.0)
    }

    /**
     * Anda [minutos] de música, clique a clique, e confere que cada posição
     * bate com a grade ideal dentro de [tolerancia] amostras.
     */
    private fun assertDerivaMenorQue(
        config: MetronomeConfig,
        minutos: Int,
        tolerancia: Double,
    ) {
        val ideal = MetronomeTiming.exactFramesPerBeat(config.bpm, sr) /
            config.subdivisionsPerBeat
        val totalCliques = (minutos * 60 * config.bpm * config.subdivisionsPerBeat).toLong()

        var pior = 0.0
        var indicePior = 0L
        for (k in 0L..totalCliques) {
            val real = MetronomeTiming.absoluteSubdivisionFrame(k, config, sr).toDouble()
            val erro = kotlin.math.abs(real - k * ideal)
            if (erro > pior) {
                pior = erro
                indicePior = k
            }
        }

        assertTrue(
            "deriva de ${"%.3f".format(pior)} amostras no clique $indicePior " +
                "(${config.bpm} BPM, ${config.beatsPerBar}/${config.subdivisionsPerBeat}) — " +
                "passou de meia amostra, o que já é audível",
            pior <= tolerancia,
        )
    }

    // --- Posição no compasso ---------------------------------------------------

    @Test
    fun `o indice reinicia a cada compasso`() {
        val config = MetronomeConfig(beatsPerBar = 4, subdivisionsPerBeat = 2)
        assertEquals(8, config.subdivisionsPerBar)
        listOf(0L to 0, 1L to 1, 7L to 7, 8L to 0, 9L to 1).forEach { (contador, esperado) ->
            assertEquals(
                "contador $contador",
                esperado,
                MetronomeTiming.localIndexInBar(contador, config),
            )
        }
    }

    @Test
    fun `o tempo dentro do compasso conta corretamente`() {
        val config = MetronomeConfig(beatsPerBar = 4, subdivisionsPerBeat = 2)
        assertEquals(0, MetronomeTiming.localBeatInBar(0, config))
        assertEquals(0, MetronomeTiming.localBeatInBar(1, config))
        assertEquals(1, MetronomeTiming.localBeatInBar(2, config))
        assertEquals(3, MetronomeTiming.localBeatInBar(7, config))
    }

    @Test
    fun `compasso de sete oito tem sete tempos`() {
        val config = MetronomeConfig(beatsPerBar = 7)
        assertEquals(7, config.subdivisionsPerBar)
        assertEquals(6, MetronomeTiming.localBeatInBar(6, config))
        assertEquals(0, MetronomeTiming.localBeatInBar(7, config))
    }

    @Test
    fun `numeracao de compassos`() {
        val config = MetronomeConfig(beatsPerBar = 4, subdivisionsPerBeat = 2)
        assertEquals(0L, MetronomeTiming.barNumber(0, config))
        assertEquals(0L, MetronomeTiming.barNumber(7, config))
        assertEquals(1L, MetronomeTiming.barNumber(8, config))
        assertEquals(10L, MetronomeTiming.barNumber(80, config))
    }

    // --- Acentos ---------------------------------------------------------------

    @Test
    fun `a primeira subdivisao de cada tempo leva acento`() {
        val config = MetronomeConfig(beatsPerBar = 4, subdivisionsPerBeat = 2)
        listOf(0L, 2L, 4L, 6L).forEach {
            assertTrue("clique $it deveria levar acento", MetronomeTiming.isAccent(it, config))
        }
        listOf(1L, 3L, 5L, 7L).forEach {
            assertTrue("clique $it não deveria levar acento", !MetronomeTiming.isAccent(it, config))
        }
    }

    @Test
    fun `o acento nao depende de contar cliques`() {
        // Em 7/8 com subdivisão, "acento a cada 4 cliques" daria um resultado
        // que não marca os tempos. O critério correto é o do tempo, não o da
        // contagem de cliques.
        val config = MetronomeConfig(beatsPerBar = 7, subdivisionsPerBeat = 2)
        listOf(0L, 2L, 4L, 12L, 14L).forEach {
            assertTrue("clique $it", MetronomeTiming.isAccent(it, config))
        }
        listOf(1L, 3L, 13L).forEach {
            assertTrue("clique $it", !MetronomeTiming.isAccent(it, config))
        }
    }

    @Test
    fun `acento a cada dois tempos`() {
        val config = MetronomeConfig(beatsPerBar = 4, accentEvery = 2)
        assertTrue(MetronomeTiming.isAccent(0, config))
        assertTrue(!MetronomeTiming.isAccent(1, config))
        assertTrue(MetronomeTiming.isAccent(2, config))
        assertTrue(!MetronomeTiming.isAccent(3, config))
    }

    @Test
    fun `o acento sobrevive a varios compassos`() {
        // Com 4/4 e acento a cada 2 tempos, o padrão se repete de 4 em 4
        // compassos. Aqui se confere que ele não "anda" ao longo do tempo.
        val config = MetronomeConfig(beatsPerBar = 4, subdivisionsPerBeat = 2, accentEvery = 2)
        val acentuados = (0L until 64L).filter { MetronomeTiming.isAccent(it, config) }
        // 16 compassos x 2 acentos
        assertEquals(32, acentuados.size)
    }

    // --- Limites ---------------------------------------------------------------

    @Test
    fun `duracao do clique em amostras`() {
        assertEquals(1_440, MetronomeTiming.clickFrames(30, sr))
        assertEquals(2_160, MetronomeTiming.clickFrames(45, sr))
        // Duração zero não pode gerar array vazio e estourar na cópia.
        assertEquals(1, MetronomeTiming.clickFrames(0, sr))
        assertEquals(1, MetronomeTiming.clickFrames(-5, sr))
    }

    @Test
    fun `bpm e arredondado para inteiro e limitado`() {
        assertEquals(120f, MetronomeTiming.snapBpm(119.6f))
        assertEquals(121f, MetronomeTiming.snapBpm(120.4f))
        assertEquals(MetronomeConfig.MIN_BPM, MetronomeTiming.snapBpm(1f))
        assertEquals(MetronomeConfig.MAX_BPM, MetronomeTiming.snapBpm(9999f))
    }

    @Test
    fun `configuracao invalida e rejeitada`() {
        listOf(
            { MetronomeConfig(bpm = 10f) },
            { MetronomeConfig(bpm = 500f) },
            { MetronomeConfig(beatsPerBar = 0) },
            { MetronomeConfig(beatsPerBar = 99) },
            { MetronomeConfig(subdivisionsPerBeat = 0) },
            { MetronomeConfig(subdivisionsPerBeat = 9) },
            { MetronomeConfig(beatsPerBar = 2, accentEvery = 3) },
        ).forEach { bloco ->
            var lancou = false
            try {
                bloco()
            } catch (e: IllegalArgumentException) {
                lancou = true
            }
            assertTrue("configuração inválida deveria ser rejeitada", lancou)
        }
    }

    @Test
    fun `os extremos de bpm continuam produzindo tempos crescentes`() {
        listOf(MetronomeConfig.MIN_BPM, MetronomeConfig.MAX_BPM).forEach { bpm ->
            val config = MetronomeConfig(bpm = bpm, subdivisionsPerBeat = 4)
            var anterior = 0L
            for (k in 1L..200L) {
                val atual = MetronomeTiming.absoluteSubdivisionFrame(k, config, sr)
                assertTrue("BPM $bpm: posição $k não avançou", atual > anterior)
                anterior = atual
            }
        }
    }
}