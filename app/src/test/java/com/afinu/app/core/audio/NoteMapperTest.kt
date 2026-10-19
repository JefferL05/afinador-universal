package com.afinu.app.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMapperTest {

    private val a4 = 440f

    @Test
    fun `A4 com referencia padrao`() {
        val leitura = NoteMapper.map(440f, a4)

        assertNotNull(leitura)
        assertEquals(69, leitura!!.midi)
        assertEquals("A", leitura.letter)
        assertEquals(4, leitura.octave)
        assertEquals(0f, leitura.cents, 0.01f)
        assertTrue(leitura.inTune)
    }

    @Test
    fun `meio tom acima de A4`() {
        // 1200 * log2(466.16 / 440) = 100 cents, a distância de um semitão.
        val leitura = NoteMapper.map(466.16f, a4)

        assertNotNull(leitura)
        assertEquals(70, leitura!!.midi)
        assertEquals("A#", leitura.letter)
        assertEquals(1, leitura.accidental)
        assertEquals(4, leitura.octave)
        assertEquals(0f, leitura.cents, 1f)
    }

    @Test
    fun `desvio tem sinal correto`() {
        val abaixo = NoteMapper.map(435f, a4)!!
        val acima = NoteMapper.map(445f, a4)!!

        assertTrue("abaixo deve ser negativo, veio ${abaixo.cents}", abaixo.cents < 0f)
        assertTrue("acima deve ser positivo, veio ${acima.cents}", acima.cents > 0f)
        assertFalse(abaixo.inTune)
        assertFalse(acima.inTune)
    }

    @Test
    fun `zona no ponto tem dois cents`() {
        assertTrue(NoteMapper.map(440f * 1.00058f, a4)!!.inTune) // ~1 cent
        assertFalse(NoteMapper.map(440f * 1.003f, a4)!!.inTune)   // ~5 cents
    }

    @Test
    fun `A4 alterado recalcula todas as notas`() {
        val comA4 = 415f
        val leitura = NoteMapper.map(415f, comA4)!!

        assertEquals("com A4 = 415 o tom 415 deve ser a nota A4", 69, leitura.midi)
        assertEquals(4, leitura.octave)
        assertEquals(0f, leitura.cents, 0.01f)
    }

    @Test
    fun `mesmo som lido como nota diferente conforme a referencia`() {
        // 440 Hz é A4 nas duas referências: com A4 = 432 ele fica 31,6 cents
        // acima, e meia nota é 50 cents, então a letra não muda.
        assertEquals(69, NoteMapper.map(440f, 440f)!!.midi)
        assertEquals(69, NoteMapper.map(440f, 432f)!!.midi)
        // Para a leitura virar Bb4, o tom precisa passar da metade do semitão:
        // 452 Hz com referência 432 são 76,8 cents acima do A4.
        assertEquals(70, NoteMapper.map(452f, 432f)!!.midi)
    }

    @Test
    fun `deslocamento global recentraliza a agulha`() {
        // Com +5 cents de deslocamento, um A4 puro deve aparecer 5 abaixo —
        // é assim que o usuário "compensa" um instrumento que afina alto.
        val leitura = NoteMapper.map(440f, a4, pitchOffsetCents = 5f)!!
        assertEquals(-5f, leitura.cents, 0.2f)
        assertEquals(69, leitura.midi)
    }

    @Test
    fun `frequencias invalidas sao rejeitadas`() {
        assertNull(NoteMapper.map(0f, a4))
        assertNull(NoteMapper.map(-100f, a4))
        assertNull(NoteMapper.map(Float.NaN, a4))
        assertNull(NoteMapper.map(Float.POSITIVE_INFINITY, a4))
    }

    @Test
    fun `notas fora da faixa MIDI sao limitadas`() {
        // 5 Hz é irreconhecível musicalmente, mas não pode estourar o índice.
        val leitura = NoteMapper.map(5f, a4)
        assertNotNull(leitura)
        assertTrue(leitura!!.midi in 0..127)
    }

    @Test
    fun `cobre todas as cordas do violao padrao`() {
        val violao = listOf(40, 45, 50, 55, 59, 64)
        violao.forEach { midi ->
            val hz = com.afinu.app.core.util.MusicTheory.frequencyOf(midi, a4)
            val leitura = NoteMapper.map(hz, a4)!!
            assertEquals("corda MIDI $midi", midi, leitura.midi)
            assertEquals("corda MIDI $midi deveria estar afinada", 0f, leitura.cents, 0.01f)
        }
    }
}