package com.afinu.app.core.model

/**
 * Uma afinação (padrão ou alternativa) de um instrumento de cordas.
 *
 * [midiNotes] vai sempre do grave ao agudo e o número de elementos é o número
 * de cordas — inclusive nas de 7, 8 e 12 cordas e no ukulele, que têm as notas
 * fora de ordem se lidas da corda mais aguda para a mais grave.
 */
data class TuningPreset(
    val id: String,
    val label: String,
    val midiNotes: List<Int>,
) {
    init {
        require(midiNotes.isNotEmpty()) { "Uma afinação precisa de pelo menos uma corda" }
        require(midiNotes.all { it in 0..127 }) { "Nota MIDI inválida em $id" }
    }

    val stringCount: Int get() = midiNotes.size

    val lowestMidi: Int get() = midiNotes.min()
    val highestMidi: Int get() = midiNotes.max()

    /**
     * Move todas as cordas em [semitones] semitons.
     *
     * É a operação que o simulador de capo usa: um capo na 3ª casa transpõe
     * a afinação inteira em +3 semitons. Como ela é uma translação pura, os
     * intervalos entre cordas **não mudam** — e é exatamente isso que faz o
     * capo não desafinar o instrumento: o problema de afinação com capo nunca é
     * intervalo, é a altura de referência.
     *
     * @throws IllegalArgumentException se alguma corda sair da faixa MIDI, o
     *   que indicaria um valor de capo absurdo para aquele instrumento.
     */
    fun shifted(semitones: Int): TuningPreset {
        if (semitones == 0) return this
        require(semitones in SHIFT_RANGE) {
            "Transposição de $semitones semitons fora da faixa permitida $SHIFT_RANGE"
        }
        val novas = midiNotes.map { it + semitones }
        require(novas.all { it in 0..127 }) {
            "Capo em $semitones semitons levaria uma corda para fora da faixa MIDI: $novas"
        }
        return copy(
            id = "$id+$semitones",
            label = "+$semitones semitons",
            midiNotes = novas,
        )
    }

    /**
     * Nome legível da afinação já com o capo aplicado, ex.: "Padrão (capo 3)".
     */
    fun labelWithCapo(capoFret: Int): String =
        if (capoFret == 0) label else "$label (capo $capoFret)"

    companion object {
        /** Faixa de transposição aceita por [shifted]. -24 a +24 cobre qualquer uso real. */
        val SHIFT_RANGE = -24..24
    }
}