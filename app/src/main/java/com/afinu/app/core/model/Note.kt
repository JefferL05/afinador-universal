package com.afinu.app.core.model

/**
 * Nota musical como o app a exibe ao usuário.
 *
 * O valor [midi] é a verdade canônica: C4 (dó agudo) é MIDI 60. Nome e oitava
 * são derivados dele, nunca armazenados — assim mudar o A4 de referência
 * recalcula tudo sem inconsistência.
 */
data class Note(
    val midi: Int,
) {
    init {
        require(midi in 0..127) { "Nota MIDI fora da faixa 0..127: $midi" }
    }

    /** Índice 0..11 dentro da oitava: C=0, C#=1, D=2 ... B=11. */
    val semitoneFromC: Int get() = midi % 12

    /** 0..10 para A=440, que é a faixa de referência padrão do afinador. */
    val octave: Int get() = midi / 12 - 1

    /** "C", "C#", "D" ... */
    val letter: String get() = NOTE_NAMES[semitoneFromC]

    /** Sustenido (1), bemol (-1) ou natural (0). */
    val accidental: Int get() = when (semitoneFromC) {
        1, 3, 6, 8, 10 -> 1
        else -> 0
    }

    /** Apenas as teclas naturais — usado no cartão de notas e no fretboard. */
    val isNatural: Boolean get() = accidental == 0

    override fun toString(): String = "$letter$octave"
}

val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")