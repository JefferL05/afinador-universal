package com.afinu.app.core.util

import com.afinu.app.core.model.NOTE_NAMES
import kotlin.math.ln
import kotlin.math.roundToInt

private val LN_2 = ln(2.0)

/**
 * Conversões entre frequência, nota MIDI e cents.
 *
 * Convenções: MIDI 69 = A4. Com A4 = 440 Hz, MIDI 69 vale exatamente 440 Hz
 * e MIDI 60 (C4) vale 261,63 Hz.
 */
object MusicTheory {

    const val MIDI_A4 = 69
    const val DEFAULT_A4_HZ = 440f

    /** A4 de referência aceito pelo app. 415 e 466 são limites históricos usuais. */
    const val MIN_A4_HZ = 415f
    const val MAX_A4_HZ = 466f

    /** Frequência de referência de uma nota MIDI. */
    fun frequencyOf(midi: Int, a4Hz: Float = DEFAULT_A4_HZ): Float {
        require(midi in 0..127) { "Nota MIDI fora da faixa 0..127: $midi" }
        require(a4Hz > 0f) { "A4 de referência deve ser positiva" }
        return a4Hz * 2f.pow((midi - MIDI_A4) / 12f)
    }

    /** Nota MIDI (fracionária) correspondente a uma frequência. */
    fun midiFloatOf(frequencyHz: Float, a4Hz: Float = DEFAULT_A4_HZ): Float {
        require(frequencyHz > 0f) { "Frequência deve ser positiva: $frequencyHz" }
        return MIDI_A4 + 12f * log2(frequencyHz / a4Hz)
    }

    /** Nota MIDI inteira mais próxima. */
    fun nearestMidi(frequencyHz: Float, a4Hz: Float = DEFAULT_A4_HZ): Int =
        midiFloatOf(frequencyHz, a4Hz).roundToInt()

    /** Desvio em cents entre a frequência medida e a de referência. Positivo = acima. */
    fun centsBetween(frequencyHz: Float, targetHz: Float): Float {
        require(frequencyHz > 0f && targetHz > 0f) { "Frequências devem ser positivas" }
        return 1200f * ln(frequencyHz / targetHz) / LN_2.toFloat()
    }

    /** Semitons entre duas notas. Sempre positivo se [higher] > [lower]. */
    fun semitonesBetween(lowerHz: Float, higherHz: Float): Int =
        (12f * ln(higherHz / lowerHz) / LN_2.toFloat()).roundToInt()

    /** Nome semiatônico, ex.: "C4", "F#3". */
    fun labelOf(midi: Int): String {
        require(midi in 0..127) { "Nota MIDI fora da faixa 0..127: $midi" }
        return "${NOTE_NAMES[midi % 12]}${midi / 12 - 1}"
    }

    /** Nome por extenso em português, usado no feedback por voz do TalkBack. */
    fun spokenNameOf(midi: Int, a4Hz: Float = DEFAULT_A4_HZ): String {
        val name = NOTE_NAMES[midi % 12]
            .replace("C#", "Dó sustenido").replace("D#", "Ré sustenido")
            .replace("F#", "Fá sustenido").replace("G#", "Sol sustenido")
            .replace("A#", "Lá sustenido").replace("C", "Dó").replace("D", "Ré")
            .replace("E", "Mi").replace("F", "Fá").replace("G", "Sol")
            .replace("A", "Lá").replace("B", "Si")
        val hz = frequencyOf(midi, a4Hz)
        return "$name, oitava ${midi / 12 - 1}, ${"%.1f".format(hz)} hertz"
    }

    private fun log2(x: Float): Float = ln(x) / LN_2.toFloat()

    private fun Float.pow(exponent: Float): Float =
        kotlin.math.exp(ln(this) * exponent)
}