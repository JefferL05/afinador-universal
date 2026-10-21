package com.afinu.app.core.util

import com.afinu.app.core.model.TuningPreset

/**
 * Uma nota que compõe um acorde, na oitava em que ela soa.
 *
 * @param midi a nota **absoluta**. Importa: "C" sozinho não é uma nota, C2 e
 *   C4 soam diferentes. E o que o app mostra e toca tem que ser a mesma coisa
 *   que o músico lê no braço.
 * @param isRoot a tônica (a fundamental) do acorde.
 */
data class ChordTone(
    val midi: Int,
    val isRoot: Boolean,
)

/**
 * As notas que compõem um acorde, na altura em que elas soam no instrumento.
 *
 * ## Por que a oitava não pode ser inventada
 * Um acorde de Dó tem as mesmas três classes de altura — Dó, Mi, Sol — seja
 * no piano, no violão ou no violino. O que muda é a **oitava**, e é ela que
 * decide se o acorde soa grave, médio ou agudo. Mostrar "Dó · Mi · Sol" sem
 * isso é correto e inútil: o músico não sabe o que tocar.
 *
 * A resposta certa já está na mão do app — a **forma** do acorde diz
 * exatamente quais notas soam e em que oitava. Então a nota exibida é a nota
 * que está na pegada, e não uma média teórica.
 *
 * Quando não há forma (instrumento sem afinação fixa, ou um acorde que não
 * cabe na mão), a referência é a afinação: as notas entram na faixa do
 * instrumento, perto da corda mais grave.
 */
object ChordTones {

    /**
     * As notas do acorde, na altura em que soam.
     *
     * @param rootSemitones a tônica, como classe de altura (0 = Dó).
     * @param quality a qualidade do acorde, que define os intervalos.
     * @param shape a forma encontrada, quando existe. É a fonte da oitava.
     * @param tuning a afinação efetiva, usada quando não há forma.
     */
    fun of(
        rootSemitones: Int,
        quality: ChordQuality,
        shape: ChordShape? = null,
        tuning: TuningPreset? = null,
    ): List<ChordTone> {
        val raiz = ((rootSemitones % 12) + 12) % 12
        val notasDaForma = shape?.notes.orEmpty().sorted()

        return quality.semitones.map { intervalo ->
            val classe = (((raiz + intervalo) % 12) + 12) % 12

            // A nota mais grave daquela classe dentro da forma. Se o acorde
            // repete a classe (C3 e C4 na mesma pegada), a grave é a que o
            // jogador ouve primeiro — e é a que dá a ideia do voicing.
            val naForma = notasDaForma.firstOrNull {
                (((it % 12) + 12) % 12) == classe
            }

            ChordTone(
                midi = naForma ?: referencia(classe, raiz, intervalo, notasDaForma, tuning),
                isRoot = intervalo == 0,
            )
        }
    }

    /**
     * Onde a nota cai quando a forma não a contém.
     *
     * A base é o chão do instrumento — a nota mais baixa da forma quando ela
     * existe, ou uma oitava acima da corda mais grave da afinação. A tônica é
     * colocada nessa base e as demais notas sobem a partir dela, para que o
     * acorde saia como um bloco, e não espalhado.
     */
    private fun referencia(
        classe: Int,
        raiz: Int,
        intervalo: Int,
        notasDaForma: List<Int>,
        tuning: TuningPreset?,
    ): Int {
        val base = notasDaForma.firstOrNull()
            ?: tuning?.lowestMidi?.plus(12)
            ?: MIDI_C4
        val tônica = base + (((raiz - base) % 12 + 12) % 12)
        return tônica + intervalo
    }

    /** Dó4, a referência de média do piano. */
    private const val MIDI_C4 = 60
}
