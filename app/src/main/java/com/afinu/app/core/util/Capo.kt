package com.afinu.app.core.util

import com.afinu.app.core.model.Instrument
import com.afinu.app.core.model.TuningPreset

/**
 * Simulador de capo.
 *
 * ## O que um capo faz, e o que ele não faz
 * Um capo **não** muda o intervalo entre as cordas. Ele encurta o vibrador em
 * todas as cordas pelo mesmo tanto, o que equivale a uma transposição de N
 * semitons aplicada à afinação inteira. Por isso [TuningPreset.shifted] é uma
 * translação simples e os intervalos permanecem intactos.
 *
 * ## Consequência prática para o afinador
 * Com capo na 3ª casa num violão em afinação padrão, a 6ª corda afinada em E2
 * soa G2. O afinador deve mirar **G2**, não E2 — é o que o músico ouve e é com
 * isso que ele compara os outros instrumentos. Por isso a afinação efetiva é a
 * que entra no medidor, e a afinação a céu aberto aparece como rótulo
 * secundário.
 *
 * ## Quem não usa capo
 * Capo em casa 0 reduz tudo a uma translação de zero, que [TuningPreset.shifted]
 * resolve devolvendo a própria instância — sem custo e sem alocação.
 */
object Capo {

    const val MIN_FRET = 0

    /** Acima de 12 o uso é raro e a afinação resultante fica implausível para a maioria. */
    const val MAX_FRET = 12

    const val NONE = 0

    /** Lista de casas para os seletores da interface. */
    val frets: List<Int> = (MIN_FRET..MAX_FRET).toList()

    fun clamp(fret: Int): Int = fret.coerceIn(MIN_FRET, MAX_FRET)

    fun isInUse(fret: Int): Boolean = fret != NONE

    /**
     * Afinação efetiva com o capo aplicado. Casa 0 devolve a afinação original.
     */
    fun effective(tuning: TuningPreset, capoFret: Int): TuningPreset =
        if (capoFret == NONE) tuning else tuning.shifted(clamp(capoFret))

    /**
     * Mesma coisa, porém nunca lança: se a casa for absurda para o instrumento,
     * cai na afinação sem capo em vez de derrubar a tela.
     */
    fun effectiveOrUnshifted(tuning: TuningPreset, capoFret: Int): TuningPreset = try {
        effective(tuning, capoFret)
    } catch (e: IllegalArgumentException) {
        tuning
    }

    /**
     * Nome da nota que a corda soa com o capo, para o rótulo "1ª corda: E2 → G2".
     */
    fun soundedNote(openMidi: Int, capoFret: Int): Int {
        val destino = openMidi + clamp(capoFret)
        return destino.coerceIn(0, 127)
    }

    /**
     * Tonalidade resultante com a afinação e o capo. Útil para o cartão de notas
     * e para mostrar em qual tom o instrumento ficou.
     *
     * Usa a nota da 6ª (mais grave) como referência, que é a convenção para
     * nomear a tonalidade de um instrumento afinado.
     */
    fun resultingTonic(tuning: TuningPreset, capoFret: Int): Int =
        soundedNote(tuning.midiNotes.min(), capoFret)

    /**
     * Verifica se a casa de capo produz uma afinação plausível para o
     * instrumento. Usado para avisar antes de salvar um favorito.
     */
    fun isPlausible(instrument: Instrument, tuning: TuningPreset, capoFret: Int): Boolean = try {
        effective(tuning, capoFret)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}