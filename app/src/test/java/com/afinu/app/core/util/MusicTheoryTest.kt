package com.afinu.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicTheoryTest {

    @Test
    fun `tabela de frequencias de referencia`() {
        // Valores da tabela padrão (A4 = 440 Hz). Erro tolerado: 0,01 Hz, que
        // bem menor que um cent na maior parte da faixa.
        val tabela = mapOf(
            40 to 82.4069f,   // E2
            45 to 110.000f,   // A2
            50 to 146.832f,   // D3
            55 to 195.998f,   // G3
            59 to 246.942f,   // B3
            60 to 261.626f,   // C4
            64 to 329.628f,   // E4
            69 to 440.000f,   // A4
            76 to 659.255f,   // E5
            81 to 880.000f,   // A5
        )
        tabela.forEach { (midi, hz) ->
            assertEquals("MIDI $midi", hz, MusicTheory.frequencyOf(midi, 440f), 0.01f)
        }
    }

    @Test
    fun `ida e volta entre MIDI e frequencia`() {
        (0..127).forEach { midi ->
            val hz = MusicTheory.frequencyOf(midi, 440f)
            assertEquals("MIDI $midi", midi, MusicTheory.nearestMidi(hz, 440f))
        }
    }

    @Test
    fun `intervalos de semitons`() {
        // Quarta justa do violão (E2 -> A2). 5 semitons, não 7: de E até A são
        // duas casas de meio tom a mais que de C até F.
        assertEquals(5, MusicTheory.semitonesBetween(
            MusicTheory.frequencyOf(40), MusicTheory.frequencyOf(45)
        ))
        // Quarta justa do violão (A2 -> D3).
        assertEquals(5, MusicTheory.semitonesBetween(
            MusicTheory.frequencyOf(45), MusicTheory.frequencyOf(50)
        ))
        // Terça maior do violão (G3 -> B3) é o único intervalo de 4 semitons.
        assertEquals(4, MusicTheory.semitonesBetween(
            MusicTheory.frequencyOf(55), MusicTheory.frequencyOf(59)
        ))
        // Oitava.
        assertEquals(12, MusicTheory.semitonesBetween(
            MusicTheory.frequencyOf(52), MusicTheory.frequencyOf(64)
        ))
        // Quinta justa do baixo: E1 -> B1, 7 semitons. (E1 -> A1 seria uma
        // quarta, e foi por ler A1 como B1 que este teste chegou a esperar 7.)
        assertEquals(7, MusicTheory.semitonesBetween(
            MusicTheory.frequencyOf(28), MusicTheory.frequencyOf(35)
        ))
    }

    @Test
    fun `cents entre frequencias`() {
        assertEquals(0f, MusicTheory.centsBetween(440f, 440f), 0.001f)
        assertEquals(100f, MusicTheory.centsBetween(466.164f, 440f), 0.01f)
        assertEquals(-100f, MusicTheory.centsBetween(415.305f, 440f), 0.01f)
        assertEquals(1200f, MusicTheory.centsBetween(880f, 440f), 0.01f)
    }

    @Test
    fun `A4 de referencia desloca toda a tabela`() {
        // Com A4 = 432, os 440 Hz do padrão ficam 31,6 cents acima do A4. Isso
        // é menos da metade de um semitone, então a nota continua sendo A4 —
        // virar "si sustenido" exigiria passar de 50 cents, ou seja, uns 451 Hz.
        assertEquals(69, MusicTheory.nearestMidi(440f, 432f))
        assertEquals(69, MusicTheory.nearestMidi(432f, 432f))
        // Acima da metade do semitone, sim: 452 Hz com A4 = 432 é Bb4.
        assertEquals(70, MusicTheory.nearestMidi(452f, 432f))
        // Baixar a referencia de 440 para 432 baixa TODAS as notas na mesma
        // proporcao: 1200*log2(432/440) = -31,77 cents, para qualquer nota.
        val erro = MusicTheory.centsBetween(
            MusicTheory.frequencyOf(40, 432f), MusicTheory.frequencyOf(40, 440f)
        )
        assertEquals(-31.77f, erro, 0.05f)
    }

    @Test
    fun `nomes das notas`() {
        assertEquals("C4", MusicTheory.labelOf(60))
        assertEquals("A4", MusicTheory.labelOf(69))
        assertEquals("C#3", MusicTheory.labelOf(49))
        // MIDI 12 é o C0; o Sol logo acima (19) é G0. MIDI 31 é G1 — a conta
        // é midi/12 - 1, e 31/12 arredonda para 2.
        assertEquals("G0", MusicTheory.labelOf(19))
        assertEquals("G1", MusicTheory.labelOf(31))
        assertEquals("G9", MusicTheory.labelOf(127))
    }

    @Test
    fun `valores invalidos sao rejeitados`() {
        listOf(-1, 128, 200).forEach { midi ->
            var lancou = false
            try {
                MusicTheory.frequencyOf(midi)
            } catch (e: IllegalArgumentException) {
                lancou = true
            }
            assertTrue("MIDI $midi deveria ser rejeitado", lancou)
        }
    }
}