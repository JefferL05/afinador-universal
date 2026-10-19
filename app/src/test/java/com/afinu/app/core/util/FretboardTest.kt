package com.afinu.app.core.util

import com.afinu.app.core.model.TuningPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FretboardTest {

    private val violao = TuningPreset("padrao", "Padrão", listOf(40, 45, 50, 55, 59, 64))
    private val dropD = TuningPreset("drop_d", "Drop D", listOf(38, 45, 50, 55, 59, 64))
    private val ukulele = TuningPreset("gcea", "Padrão", listOf(67, 60, 64, 69))

    // --- A fórmula -------------------------------------------------------------

    @Test
    fun `a casa e a corda aberta mais o numero`() {
        assertEquals(40, Fretboard.midiAt(violao, 0, 0))   // E2 aberta
        assertEquals(41, Fretboard.midiAt(violao, 0, 1))   // F2
        assertEquals(52, Fretboard.midiAt(violao, 0, 12))  // E3, uma oitava
        assertEquals(64, Fretboard.midiAt(violao, 5, 0))   // E4 aberta
        assertEquals(69, Fretboard.midiAt(violao, 5, 5))   // A4
    }

    @Test
    fun `a casa 12 e exatamente uma oitava acima`() {
        violao.midiNotes.forEachIndexed { corda, aberta ->
            assertEquals("corda $corda", aberta + 12, Fretboard.midiAt(violao, corda, 12))
        }
    }

    @Test
    fun `corda ou casa fora de faixa devolve null`() {
        assertNull(Fretboard.midiAt(violao, 99, 0))
        assertNull(Fretboard.midiAt(violao, -1, 0))
        assertNull(Fretboard.midiAt(violao, 5, -3))
        // A 6ª corda (E4=64) na casa 70 passaria do MIDI 127.
        assertNull(Fretboard.midiAt(violao, 5, 70))
    }

    // --- Busca de nota ---------------------------------------------------------

    @Test
    fun `G3 aparece nas quatro posicoes esperadas no violao`() {
        // Verificado: E2+15, A2+10, D3+5, G3+0. As cordas B3 e E4 estão
        // acima de G3, então não têm posição dentro do braço.
        val esperadas = setOf(
            0 to 15, 1 to 10, 2 to 5, 3 to 0,
        )
        val achadas = Fretboard.positionsForNote(violao, 55)
            .map { it.stringIndex to it.fret }
            .toSet()

        assertEquals(esperadas, achadas)
    }

    @Test
    fun `D3 aparece nas tres posicoes esperadas`() {
        // E2+10, A2+5, D3+0.
        val achadas = Fretboard.positionsForNote(violao, 50)
            .map { it.stringIndex to it.fret }
            .toSet()
        assertEquals(setOf(0 to 10, 1 to 5, 2 to 0), achadas)
    }

    @Test
    fun `a forma aberta de G soa exatamente as notas do acorde`() {
        // 3-2-0-0-3-3 é a forma aberta de Sol no violão padrão.
        val shape = listOf(3, 2, 0, 0, 3, 3)
        val notas = shape.mapIndexed { i, casa -> violao.midiNotes[i] + casa }

        assertEquals(listOf("G2", "B2", "D3", "G3", "D4", "G4"), notas.map { MusicTheory.labelOf(it) })
        // Sol maior: Sol, Si, Ré.
        listOf(7, 11, 2).forEach { classe ->
            assertTrue(
                "faltou a classe $classe",
                notas.any { it % 12 == classe },
            )
        }
    }

    @Test
    fun `a quinta casa de cada corda da as classes que davam`() {
        // Documenta um fato que pega a intuição errada: a 5ª casa NÃO dá a mesma
        // nota em todas as cordas. São A, D, G, C, E, A. É por isso que o shape
        // aberto de Sol usa 3-2-0-0-3-3 e não "todas na 5ª".
        val esperadas = listOf("A2", "D3", "G3", "C4", "E4", "A4")
        assertEquals(
            esperadas,
            violao.midiNotes.indices.map { MusicTheory.labelOf(Fretboard.midiAt(violao, it, 5)!!) },
        )
    }

    @Test
    fun `as posicoes vem ordenadas por corda e depois por casa`() {
        val posicoes = Fretboard.positionsForNoteName(violao, "A")
        posicoes.zipWithNext { a, b ->
            assertTrue(
                "ordenação quebrada entre $a e $b",
                a.stringIndex < b.stringIndex ||
                    (a.stringIndex == b.stringIndex && a.fret <= b.fret),
            )
        }
    }

    @Test
    fun `Sol aparece nove vezes no braco`() {
        // Contagem verificada nas 6 cordas, casas 0 a 15.
        assertEquals(9, Fretboard.positionsForNoteName(violao, "G").size)
    }

    @Test
    fun `a busca por nome cobre todas as oitavas`() {
        val gs = Fretboard.positionsForNoteName(violao, "G")
        val oitavas = gs.map { it.midi / 12 }.toSet()
        assertTrue("Sol deveria aparecer em mais de uma oitava", oitavas.size >= 2)
    }

    @Test
    fun `a melhor posicao e a de menor casa`() {
        // G3: a menor casa é a 0, na 4ª corda aberta.
        val melhor = Fretboard.bestPositionForNote(violao, 55)
        assertNotNull(melhor)
        assertEquals(0, melhor!!.fret)
        assertEquals(3, melhor.stringIndex)
    }

    @Test
    fun `nome invalido devolve lista vazia em vez de estourar`() {
        assertTrue(Fretboard.positionsForNoteName(violao, "H").isEmpty())
        assertTrue(Fretboard.positionsForNoteName(violao, "").isEmpty())
        assertTrue(Fretboard.positionsForNoteName(violao, "Cb#").isEmpty())
    }

    // --- A afinação muda tudo --------------------------------------------------

    @Test
    fun `drop D move a posicao de D3 na 6a corda`() {
        // Padrão: E2 + 10 = D3. Drop D: D2 + 12 = D3. A 6ª corda é E4 (64) e
        // está acima de D3 — quem sobe até lá é a 1ª corda.
        assertEquals(10, Fretboard.positionsForNote(violao, 50).first { it.stringIndex == 0 }.fret)
        assertEquals(12, Fretboard.positionsForNote(dropD, 50).first { it.stringIndex == 0 }.fret)
    }

    @Test
    fun `com capo na terceira o braco inteiro sobe tres semitons`() {
        val comCapo = Capo.effective(violao, 3)
        for (corda in violao.midiNotes.indices) {
            for (casa in 0..12) {
                assertEquals(
                    "corda $corda casa $casa",
                    Fretboard.midiAt(violao, corda, casa)!! + 3,
                    Fretboard.midiAt(comCapo, corda, casa)!!,
                )
            }
        }
    }

    // --- Formato completo ------------------------------------------------------

    @Test
    fun `o mapa completo cobre todas as cordas e casas`() {
        assertEquals(6 * 16, Fretboard.positions(violao, 0, 15).size)
    }

    @Test
    fun `uma faixa de casas produz a contagem esperada`() {
        assertEquals(6 * 5, Fretboard.positions(violao, 5, 9).size)
    }

    @Test
    fun `faixa invalida e rejeitada`() {
        var lancou = false
        try {
            Fretboard.positions(violao, 5, 3)
        } catch (e: IllegalArgumentException) {
            lancou = true
        }
        assertTrue("faixa invertida deveria ser rejeitada", lancou)
    }

    // --- Nomes -----------------------------------------------------------------

    @Test
    fun `nomes de nota viram semitons`() {
        assertEquals(0, Fretboard.semitonesFromName("C"))
        assertEquals(2, Fretboard.semitonesFromName("D"))
        assertEquals(4, Fretboard.semitonesFromName("E"))
        assertEquals(7, Fretboard.semitonesFromName("G"))
        assertEquals(1, Fretboard.semitonesFromName("C#"))
        assertEquals(11, Fretboard.semitonesFromName("B"))
        // B# é o mesmo som que o Dó: o semitonal de B# é 0, não 11.
        assertEquals(0, Fretboard.semitonesFromName("B#"))
        assertEquals(10, Fretboard.semitonesFromName("Bb"))
        assertEquals(6, Fretboard.semitonesFromName("F#"))
    }

    @Test
    fun `nomes minusculos e com simbolos unicode`() {
        assertEquals(1, Fretboard.semitonesFromName("c#"))
        assertEquals(6, Fretboard.semitonesFromName("F♯"))
        // Lá é a 9ª classe (A). O bemol desce um semitom: 8. Confundir o
        // símbolo ♭ com a letra B faria "A♭" virar um acorde de Si.
        assertEquals(8, Fretboard.semitonesFromName("A♭"))
    }

    @Test
    fun `o ukulele da Sol na terceira casa da terceira corda`() {
        // 3ª corda do ukulele é Mi4 (64). 64 + 3 = 67 = Sol4.
        assertEquals(67, Fretboard.midiAt(ukulele, 2, 3))
        assertTrue(
            Fretboard.positionsForNoteName(ukulele, "G")
                .any { it.stringIndex == 2 && it.fret == 3 },
        )
    }
}