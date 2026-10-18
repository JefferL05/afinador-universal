package com.afinu.app.feature.tuner

import com.afinu.app.core.model.TuningPreset
import com.afinu.app.core.util.MusicTheory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerLogicTest {

    private val violao = TuningPreset("padrao", "Padrão", listOf(40, 45, 50, 55, 59, 64))

    @Test
    fun `identifica a corda quando cada uma toca na nota`() {
        violao.midiNotes.forEachIndexed { indice, midi ->
            val hz = MusicTheory.frequencyOf(midi, 440f)
            assertEquals("corda MIDI $midi", indice, TunerLogic.detectString(violao, hz, 440f) ?: -1)
        }
    }

    @Test
    fun `identifica a corda mesmo com desvio de dez cents`() {
        violao.midiNotes.forEachIndexed { indice, midi ->
            val hz = MusicTheory.frequencyOf(midi, 440f) * Math.pow(2.0, 10.0 / 1200.0).toFloat()
            assertEquals("corda MIDI $midi com +10 cents", indice, TunerLogic.detectString(violao, hz, 440f) ?: -1)
        }
    }

    @Test
    fun `a terceira corda do ukulele nao confunde com a quarta`() {
        // Ukulele padrão: G4 C4 E4 A4 = MIDI 67, 60, 64, 69.
        // E3 e A4 distam 5 semitons, e o intervalo C4->E4 é de 4: a nota mais
        // próxima não resolveria, o desvio em cents resolve.
        val ukulele = TuningPreset("gcea", "Padrão", listOf(67, 60, 64, 69))
        ukulele.midiNotes.forEachIndexed { indice, midi ->
            val hz = MusicTheory.frequencyOf(midi, 440f)
            assertEquals("corda MIDI $midi", indice, TunerLogic.detectString(ukulele, hz, 440f) ?: -1)
        }
    }

    @Test
    fun `frequencia invalida nao devolve corda`() {
        assertNull(TunerLogic.detectString(violao, 0f, 440f))
        assertNull(TunerLogic.detectString(violao, Float.NaN, 440f))
        assertNull(TunerLogic.detectString(violao, -50f, 440f))
    }

    @Test
    fun `desvio para a corda correta`() {
        val hz = MusicTheory.frequencyOf(59, 440f) * Math.pow(2.0, 7.0 / 1200.0).toFloat()

        assertEquals(7f, TunerLogic.centsToString(violao, 4, hz, 440f), 0.1f)
        // Cordas vizinhas estão muito mais longe em cents.
        assertTrue(kotlin.math.abs(TunerLogic.centsToString(violao, 3, hz, 440f)) > 100f)
    }

    @Test
    fun `indice de corda invalido lanca`() {
        var lancou = false
        try {
            TunerLogic.centsToString(violao, 99, 440f, 440f)
        } catch (e: IllegalArgumentException) {
            lancou = true
        }
        assertTrue("índice de corda inválido deveria lançar", lancou)
    }

    @Test
    fun `modo guiado so avanca com a corda pedida E afinada`() {
        // Destino errado: não avança.
        assertEquals(false, TunerLogic.shouldAdvanceGuided(guidedIndex = 2, detectedIndex = 1, inTune = true))
        // Corda certa mas desafinada: não avança.
        assertEquals(false, TunerLogic.shouldAdvanceGuided(guidedIndex = 2, detectedIndex = 2, inTune = false))
        // Corda certa e afinada: avança.
        assertEquals(true, TunerLogic.shouldAdvanceGuided(guidedIndex = 2, detectedIndex = 2, inTune = true))
        // Modo normal (sem guia): nunca avança.
        assertEquals(false, TunerLogic.shouldAdvanceGuided(guidedIndex = null, detectedIndex = 2, inTune = true))
    }
}