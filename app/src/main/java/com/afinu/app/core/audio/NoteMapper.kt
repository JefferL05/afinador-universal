package com.afinu.app.core.audio

import com.afinu.app.core.model.NOTE_NAMES
import com.afinu.app.core.util.MusicTheory
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

private val LN_2 = ln(2.0).toFloat()

/** Tudo que o medidor precisa mostrar para uma única leitura de tom. */
data class NoteReading(
    /** Nota MIDI mais próxima do tom detectado. */
    val midi: Int,
    /** Nome semiatônico, ex.: "C", "F#". */
    val letter: String,
    /** 1 = sustenido, -1 = bemol, 0 = natural. */
    val accidental: Int,
    /** Oitava. C4 = 4 na convenção do app. */
    val octave: Int,
    /** Frequência efetivamente medida. */
    val frequencyHz: Float,
    /** Frequência que a nota deveria ter. */
    val targetFrequencyHz: Float,
    /** Desvio em cents. Negativo = abaixo da nota. */
    val cents: Float,
) {
    /** Dentro da zona de tolerância — é o que aciona o bip de confirmação. */
    val inTune: Boolean get() = abs(cents) <= NoteMapper.IN_TUNE_CENTS

    /** Identificador estável para animações e histórico. */
    val key: String get() = "$letter$octave"

    companion object {
        /** Zona "no ponto" do medidor. 2 cents é perceptível só com o afinador. */
        const val IN_TUNE_CENTS = 2f
    }
}

/**
 * Converte frequência em leitura de nota.
 *
 * O deslocamento global em cents ([pitchOffsetCents]) desloca o **referencial**,
 * não a medição: o usuário marca "+3 cents" e a agulha centraliza quando o
 * instrumento estiver 3 cents acima da temperamento igual. É assim que um
 * afinador físico com botão de afinação fina se comporta.
 */
object NoteMapper {

    const val IN_TUNE_CENTS = NoteReading.IN_TUNE_CENTS

    /**
     * @param frequencyHz frequência medida pelo detector.
     * @param a4Hz A4 de referência (415 a 466 Hz).
     * @param pitchOffsetCents deslocamento global do referencial, em cents.
     */
    fun map(
        frequencyHz: Float,
        a4Hz: Float = MusicTheory.DEFAULT_A4_HZ,
        pitchOffsetCents: Float = 0f,
    ): NoteReading? {
        if (!frequencyHz.isFinite() || frequencyHz <= 0f) return null

        // 2^(c/1200) reescrito como exp, evitando pow em ponto flutuante.
        val a4Efetivo = a4Hz * exp(pitchOffsetCents / 1200f * LN_2)

        val midiFloat = MusicTheory.midiFloatOf(frequencyHz, a4Efetivo)
        val midi = midiFloat.roundToInt().coerceIn(0, 127)
        val target = MusicTheory.frequencyOf(midi, a4Efetivo)

        return NoteReading(
            midi = midi,
            letter = NOTE_NAMES[midi % 12],
            accidental = when (midi % 12) {
                1, 3, 6, 8, 10 -> 1
                else -> 0
            },
            octave = midi / 12 - 1,
            frequencyHz = frequencyHz,
            targetFrequencyHz = target,
            cents = MusicTheory.centsBetween(frequencyHz, target),
        )
    }
}