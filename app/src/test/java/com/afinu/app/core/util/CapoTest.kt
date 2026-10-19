package com.afinu.app.core.util

import com.afinu.app.core.model.TuningPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CapoTest {

    private val violao = TuningPreset("padrao", "Padrão", listOf(40, 45, 50, 55, 59, 64))

    // --- A propriedade central: o capo transpõe, não desafina ------------------

    @Test
    fun `capo na terceira casa vira Sol maior no violao`() {
        // E2 + 3 semitons = G2. Esta é a afinação clássica de "violão com capo
        // na terceira", e é ela que o medidor tem de mirar.
        val comCapo = Capo.effective(violao, 3)
        assertEquals(listOf(43, 48, 53, 58, 62, 67), comCapo.midiNotes)
        assertEquals("G2", MusicTheory.labelOf(comCapo.midiNotes.min()))
    }

    @Test
    fun `os intervalos entre cordas nao mudam com capo`() {
        // Esta é a razão de [TuningPreset.shifted] ser uma translação simples:
        // o capo encurta o vibrador em todas as cordas pelo mesmo tanto.
        listOf(0, 1, 3, 5, 7, 12).forEach { fret ->
            val aberta = violao.midiNotes.zipWithNext { a, b -> b - a }
            val comCapo = Capo.effective(violao, fret).midiNotes.zipWithNext { a, b -> b - a }
            assertEquals("capo na casa $fret alterou um intervalo", aberta, comCapo)
        }
    }

    @Test
    fun `capo zero devolve a propria instancia sem alocar`() {
        val resultado = Capo.effective(violao, Capo.NONE)
        assertSame("casa 0 não deve criar uma cópia", violao, resultado)
    }

    @Test
    fun `cada casa produz uma nota distinta`() {
        val notas = Capo.frets.map { MusicTheory.labelOf(Capo.resultingTonic(violao, it)) }
        assertEquals("casas repetidas produziram a mesma nota", notas.size, notas.toSet().size)
    }

    @Test
    fun `todas as casas do violao cabem na faixa MIDI`() {
        (Capo.MIN_FRET..Capo.MAX_FRET).forEach { fret ->
            val resultado = Capo.effectiveOrUnshifted(violao, fret)
            assertEquals("casa $fret", 6, resultado.stringCount)
            assertTrue("casa $fret saiu da faixa", resultado.midiNotes.all { it in 0..127 })
        }
    }

    // --- A Property do ukulele, que é onde o capo mais aparece ---------------

    @Test
    fun `ukulele com capo na segunda continua ukulele`() {
        // Cords do grave para o agudo: Dó4, Mi4, Sol4, Lá4.
        val ukulele = TuningPreset("gcea", "Padrão", listOf(60, 64, 67, 69))
        val comCapo = Capo.effective(ukulele, 2)
        assertEquals(listOf(62, 66, 69, 71), comCapo.midiNotes)
        // ukulele é A4: A + 2 semitons = B
        assertEquals("B4", MusicTheory.labelOf(comCapo.midiNotes.max()))
    }

    // --- Casos limite ----------------------------------------------------------

    @Test
    fun `casa fora da faixa e limitada`() {
        assertEquals(Capo.MIN_FRET, Capo.clamp(-5))
        assertEquals(Capo.MAX_FRET, Capo.clamp(99))
        assertEquals(3, Capo.clamp(3))
    }

    @Test
    fun `transposicao absurda e rejeitada em vez de quebrar a tela`() {
        // O setter de ajustes nunca deixa passar, mas um import de JSON pode.
        // A API estrita avisa; a tolerante, abaixo, é quem não quebra a tela.
        val ukulele = TuningPreset("gcea", "Padrão", listOf(60, 64, 67, 69))
        var lancou = false
        try {
            Capo.effective(ukulele, 24)
        } catch (e: IllegalArgumentException) {
            lancou = true
        }
        assertTrue("transposição de 24 semitons deveria ser rejeitada", lancou)
    }

    @Test
    fun `versao tolerante devolve a afinacao original`() {
        val ukulele = TuningPreset("gcea", "Padrão", listOf(60, 64, 67, 69))
        assertEquals(ukulele, Capo.effectiveOrUnshifted(ukulele, 24))
    }

    @Test
    fun `nota soada fica dentro da faixa mesmo com entrada absurda`() {
        // Casa 12 sobre a corda mais aguda do violão (E4 = 64) dá 76, que cabe.
        // Para estourar o topo é preciso um capo absurdo, que [clamp] corta.
        assertEquals(127, Capo.soundedNote(120, 12))
        // MIDI 0 com capo na 12 soa MIDI 12: o capo encurta o vibrador, ele não
        // empurra a nota para baixo.
        assertEquals(12, Capo.soundedNote(0, 12))
        assertEquals(12, Capo.soundedNote(0, 99))
        assertEquals(64, Capo.soundedNote(64, 0))
    }

    @Test
    fun `capo em uso`() {
        assertFalse(Capo.isInUse(0))
        assertTrue(Capo.isInUse(1))
    }

    @Test
    fun `rotulo da afinacao inclui o capo apenas quando existe`() {
        assertEquals("Padrão", violao.labelWithCapo(0))
        assertEquals("Padrão (capo 3)", violao.labelWithCapo(3))
    }

    // --- TuningPreset.shifted --------------------------------------------------

    @Test
    fun `shifted gera id unico`() {
        val deslocada = violao.shifted(3)
        assertNotEquals(violao.id, deslocada.id)
        assertEquals("padrao+3", deslocada.id)
    }

    @Test
    fun `shifted com zero semitons devolve a mesma instancia`() {
        assertSame(violao, violao.shifted(0))
    }

    @Test
    fun `shifted preserva a contagem de cordas`() {
        listOf(0, 1, 5, 12).forEach { fret ->
            assertEquals(
                "capo na casa $fret",
                violao.stringCount,
                violao.shifted(fret).stringCount,
            )
        }
    }

    @Test
    fun `shifted para baixo tambem funciona`() {
        val abaixo = violao.shifted(-2)
        assertEquals(38, abaixo.midiNotes.min())
    }

    @Test
    fun `shifted fora da faixa de semitons e rejeitado`() {
        var lancou = false
        try {
            violao.shifted(99)
        } catch (e: IllegalArgumentException) {
            lancou = true
        }
        assertTrue("transposição de 99 semitons deveria ser rejeitada", lancou)
    }
}