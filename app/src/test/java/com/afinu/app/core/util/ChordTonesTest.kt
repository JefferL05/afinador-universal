package com.afinu.app.core.util

import com.afinu.app.core.model.TuningPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * As notas que a tela de acordes mostra e toca.
 *
 * O ponto não é a classe de altura — "C, E, G" é fácil. É a **oitava**: a
 * nota exibida tem que ser a que soa na pegada, senão o músico lê uma coisa e
 * ouve outra.
 */
class ChordTonesTest {

    private val violao = TuningPreset("padrao", "Padrão", listOf(40, 45, 50, 55, 59, 64))
    private val ukulele = TuningPreset("gcea", "Padrão", listOf(67, 60, 64, 69))

    @Test
    fun `as notas mostradas sao exatamente as da pegada`() {
        val forma = ChordFinder.findShapes(
            violao,
            ChordSpec(rootMidi = 60, quality = ChordQuality.MAJOR),
        ).first()

        val notas = ChordTones.of(
            rootSemitones = 0,
            quality = ChordQuality.MAJOR,
            shape = forma,
        )

        assertEquals(3, notas.size)
        assertTrue(
            "toda nota exibida tem de estar na pegada: $notas / ${forma.notes}",
            notas.all { it.midi in forma.notes },
        )
    }

    @Test
    fun `a fundamental vem primeira e e a unica marcada`() {
        val notas = ChordTones.of(
            rootSemitones = 0,
            quality = ChordQuality.MAJOR,
            shape = ChordFinder.findShapes(
                violao,
                ChordSpec(rootMidi = 60, quality = ChordQuality.MAJOR),
            ).first(),
        )

        assertTrue("a primeira nota é a fundamental", notas.first().isRoot)
        assertEquals(
            "só a fundamental é marcada",
            1,
            notas.count { it.isRoot },
        )
    }

    @Test
    fun `a fundamental e a nota mais grave do acorde`() {
        val notas = ChordTones.of(
            rootSemitones = 0,
            quality = ChordQuality.MAJOR,
            shape = ChordFinder.findShapes(
                violao,
                ChordSpec(rootMidi = 60, quality = ChordQuality.MAJOR),
            ).first(),
        )

        assertEquals(
            "a fundamental é a mais grave: $notas",
            notas.first().midi,
            notas.minOf { it.midi },
        )
    }

    @Test
    fun `sem forma as notas caem na faixa do instrumento`() {
        val notas = ChordTones.of(
            rootSemitones = 0,
            quality = ChordQuality.MAJOR,
            shape = null,
            tuning = violao,
        )

        assertEquals(3, notas.size)
        assertTrue(
            "nenhuma nota abaixo da corda mais grave: $notas",
            notas.all { it.midi >= violao.lowestMidi },
        )
        assertTrue(
            "nenhuma nota muito acima da corda mais aguda: $notas",
            notas.all { it.midi <= violao.highestMidi + 12 },
        )
    }

    @Test
    fun `o ukulele nao recebe notas de violao`() {
        val notas = ChordTones.of(
            rootSemitones = 0,
            quality = ChordQuality.MAJOR,
            shape = null,
            tuning = ukulele,
        )

        assertTrue(
            "nota de ukulele abaixo da faixa do ukulele: $notas",
            notas.all { it.midi >= ukulele.lowestMidi },
        )
    }

    @Test
    fun `nenhuma qualidade repete classe de altura`() {
        // Duas notas de mesma classe na mesma faixa mostrariam dois chips
        // iguais na tela, e o músico não saberia qual tocar.
        ChordQuality.entries.forEach { qualidade ->
            val notas = ChordTones.of(
                rootSemitones = 0,
                quality = qualidade,
                shape = null,
                tuning = violao,
            )
            val classes = notas.map { it.midi % 12 }
            assertEquals(
                "classe repetida em ${qualidade.suffix}: $notas",
                classes.size,
                classes.distinct().size,
            )
        }
    }

    @Test
    fun `todas as qualidades tem a fundamental`() {
        ChordQuality.entries.forEach { qualidade ->
            val notas = ChordTones.of(
                rootSemitones = 3,
                quality = qualidade,
                shape = null,
                tuning = violao,
            )
            assertEquals(
                "sem fundamental em ${qualidade.suffix}",
                1,
                notas.count { it.isRoot },
            )
        }
    }

    @Test
    fun `a nota tocada tem a frequencia da nota`() {
        val nota = ChordTones.of(
            rootSemitones = 0,
            quality = ChordQuality.MAJOR,
            shape = null,
            tuning = violao,
        ).first()

        // 261,63 Hz é o Dó4. Se a conta da frequência estivesse errada, o
        // musician ouviria uma nota e leria outra.
        assertEquals(261.63f, MusicTheory.frequencyOf(nota.midi), 0.1f)
    }

    @Test
    fun `as notas do acorde de Do soam na altura da pegada`() {
        val forma = ChordFinder.findShapes(
            violao,
            ChordSpec(rootMidi = 60, quality = ChordQuality.MAJOR),
        ).first()
        val notas = ChordTones.of(
            rootSemitones = 0,
            quality = ChordQuality.MAJOR,
            shape = forma,
        )

        // C3 = 130,81 Hz, E3 = 164,81 Hz, G3 = 196,00 Hz. São as alturas
        // reais da pegada aberta de Dó no violão — o que o app toca é o que
        // o músico vai ouvir, não uma média teórica.
        assertEquals(130.81f, MusicTheory.frequencyOf(notas[0].midi), 0.02f)
        assertEquals(164.81f, MusicTheory.frequencyOf(notas[1].midi), 0.02f)
        assertEquals(196.00f, MusicTheory.frequencyOf(notas[2].midi), 0.02f)
    }

    @Test
    fun `o A4 do usuario muda a frequencia tocada`() {
        // Com o afinador em 432 Hz, a mesma nota tem de soar mais grave do que
        // a 440 Hz. Tocar 440 fixo seria o app se contradizendo com o resto de
        // si mesmo: o medidor mostraria um Dó e o som seria outro.
        val nota = ChordTones.of(
            rootSemitones = 0,
            quality = ChordQuality.MAJOR,
            shape = null,
            tuning = violao,
        ).first()

        val em440 = MusicTheory.frequencyOf(nota.midi, 440f)
        val em432 = MusicTheory.frequencyOf(nota.midi, 432f)

        assertTrue("432 Hz tem de soar mais grave que 440 Hz", em432 < em440)
        assertEquals(em440 * 432f / 440f, em432, 0.01f)
    }

    @Test
    fun `o capo desloca as notas do acorde`() {
        // Com capo na 2ª casa o violão soa duas casas acima: o mesmo acorde
        // de Dó está um tom e meio mais agudo, e as notas exibidas têm de
        // acompanhar.
        val comCapo = violao.shifted(2)
        val notas = ChordTones.of(
            rootSemitones = 0,
            quality = ChordQuality.MAJOR,
            shape = null,
            tuning = comCapo,
        )

        assertTrue(
            "o capo não deslocou as notas: $notas",
            notas.all { it.midi >= comCapo.lowestMidi },
        )
    }

    @Test
    fun `acorde com bemol usa a classe certa`() {
        // Si bemol: a tônica é a classe 10, não 11.
        val notas = ChordTones.of(
            rootSemitones = 10,
            quality = ChordQuality.MAJOR,
            shape = null,
            tuning = violao,
        )

        assertEquals(10, notas.first().midi % 12)
        assertFalse("o La não é a fundamental de Bb", notas.first().midi % 12 == 9)
    }
}
