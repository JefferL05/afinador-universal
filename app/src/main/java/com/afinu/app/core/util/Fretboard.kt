package com.afinu.app.core.util

import com.afinu.app.core.model.TuningPreset

/**
 * Uma posição no braço do instrumento.
 *
 * [stringIndex] é 0 para a corda mais grave — a mesma ordem de
 * [TuningPreset.midiNotes]. [fret] 0 significa corda aberta.
 */
data class FretPosition(
    val stringIndex: Int,
    val fret: Int,
    val midi: Int,
    val isOpen: Boolean = fret == 0,
) {
    val noteName: String get() = MusicTheory.labelOf(midi)
    val isRoot: Boolean get() = false
}

/**
 * Mapa do braço do instrumento.
 *
 * ## O que este objeto resolve
 * O fretboard é a ferramenta para afinar "de ouvido" e para encontrar onde uma
 * nota aparece. A fórmula é uma linha — `nota = cordaAberta + casa` — mas o que
 * importa é a **busca**: dado um nome de nota, quais posições do braço tocam
 * essa nota. Errar essa busca significa mostrar a casa errada e ensinar o
 * usuário a afinar errado.
 *
 * ## Por que a afinação importa em tudo
 * Como a fórmula parte da afinação, uma posição muda inteira quando o usuário
 * troca para Drop D. Não existe "o fretboard do violão" como objeto fixo: existe
 * o fretboard **da afinação atual**. É por isso que a Fase 4 precisou produzir a
 * afinação efetiva com o capo antes de esta fase existir.
 */
object Fretboard {

    const val DEFAULT_MAX_FRET = 15

    /** Nota de uma casa do braço. Fora de faixa devolve null em vez de estourar. */
    fun midiAt(tuning: TuningPreset, stringIndex: Int, fret: Int): Int? {
        // Casa negativa não existe. Sem esta guarda, `E4 + (-3)` voltaria 61 e
        // o chamador receberia uma nota válida onde queria um erro.
        if (fret < 0) return null
        val aberta = tuning.midiNotes.getOrNull(stringIndex) ?: return null
        val nota = aberta + fret
        return if (nota in 0..127) nota else null
    }

    fun positionAt(tuning: TuningPreset, stringIndex: Int, fret: Int): FretPosition? {
        val nota = midiAt(tuning, stringIndex, fret) ?: return null
        return FretPosition(stringIndex, fret, nota)
    }

    /** Todas as posições do braço, da casa [fromFret] à [toFret]. */
    fun positions(
        tuning: TuningPreset,
        fromFret: Int = 0,
        toFret: Int = DEFAULT_MAX_FRET,
    ): List<FretPosition> {
        require(fromFret >= 0) { "Casa inicial negativa: $fromFret" }
        require(toFret >= fromFret) { "Faixa inválida: $fromFret..$toFret" }

        val resultado = ArrayList<FretPosition>()
        tuning.midiNotes.forEachIndexed { corda, aberta ->
            for (casa in fromFret..toFret) {
                val nota = aberta + casa
                if (nota in 0..127) resultado += FretPosition(corda, casa, nota)
            }
        }
        return resultado
    }

    /**
     * Todas as posições que tocam uma nota específica.
     *
     * A resposta vem da esquerda para a direita do braço, da corda mais grave
     * para a mais aguda. Essa ordem é a que o musician percorre: começa pelas
     * graves, que são onde se tira o tom de referência.
     */
    fun positionsForNote(
        tuning: TuningPreset,
        targetMidi: Int,
        fromFret: Int = 0,
        toFret: Int = DEFAULT_MAX_FRET,
    ): List<FretPosition> = positions(tuning, fromFret, toFret)
        .filter { it.midi == targetMidi }
        .sortedWith(compareBy({ it.stringIndex }, { it.fret }))

    /**
     * Todas as posições que tocam uma nota pelo **nome**, ex.: "G", "F#".
     *
     * Devolve todas as oitavas da nota. Restringir a uma oitava esconderia
     * justamente as posições que o músico procura.
     */
    fun positionsForNoteName(
        tuning: TuningPreset,
        name: String,
        fromFret: Int = 0,
        toFret: Int = DEFAULT_MAX_FRET,
    ): List<FretPosition> {
        val semitom = semitonesFromName(name) ?: return emptyList()
        return positions(tuning, fromFret, toFret)
            .filter { it.midi % 12 == semitom }
            .sortedWith(compareBy({ it.stringIndex }, { it.fret }))
    }

    /**
     * A posição mais conveniente de uma nota: a de menor casa.
     *
     * É a escolha padrão porque o jogador médio tem o braço mais curto que o
     * instrumento e começa a vibrar perto do cravelha. A casa menor é quase
     * sempre tocável; a maior raramente é.
     */
    fun bestPositionForNote(
        tuning: TuningPreset,
        targetMidi: Int,
        toFret: Int = DEFAULT_MAX_FRET,
    ): FretPosition? = positionsForNote(tuning, targetMidi, 0, toFret)
        // Ordenar por (corda, casa) e pegar o primeiro devolveria sempre a
        // corda mais grave — e o G3 do violão, por exemplo, sairia na casa 15.
        // A casa é o critério; a corda mais grave desempata, porque é onde se
        // tira o tom de referência.
        .minWithOrNull(compareBy({ it.fret }, { it.stringIndex }))

    /**
     * Intervalos de semitons entre duas posições.
     *
     * É o que permite montar acordes: um acorde é um conjunto de notas que se
     * encaixa em uma pegada, e o intervalo mede se as duas posições realmente
     * formam o intervalo pedido.
     */
    fun semitonesBetween(a: FretPosition, b: FretPosition): Int = b.midi - a.midi

    /** Converte "C", "F#", "Bb" em semitons a partir de C. Null se inválido. */
    fun semitonesFromName(name: String): Int? {
        // A letra é maiúscula, mas o sinal não: "Bb" e "B" precisam sair
        // results diferentes, e um `uppercase()` na string inteira apagaria essa
        // diferença — "Bb" viraria "BB". Por isso a letra é normalizada sozinha
        // e o segundo caractere é lido no original.
        val bruto = name.trim()
        if (bruto.isEmpty() || bruto.length > 2) return null

        val base = when (bruto[0].uppercaseChar()) {
            'C' -> 0; 'D' -> 2; 'E' -> 4; 'F' -> 5
            'G' -> 7; 'A' -> 9; 'B' -> 11
            else -> return null
        }
        if (bruto.length == 1) return base

        return when (bruto[1]) {
            '#', '♯' -> (base + 1) % 12
            'b', '♭' -> (base + 11) % 12
            else -> null
        }
    }
}