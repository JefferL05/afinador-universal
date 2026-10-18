package com.afinu.app.core.util

import com.afinu.app.core.model.TuningPreset

/**
 * Qualidade de acorde: os semitons acima da tônica que formam o acorde.
 *
 * @param suffix como o acorde é escrito depois da letra, ex.: "m7", "sus4".
 */
enum class ChordQuality(
    val suffix: String,
    val semitones: List<Int>,
) {
    /** Maior: 1, 4, 5, é a tríade básica. */
    MAJOR("", listOf(0, 4, 7)),

    /** Menor: 1, ♭3, 5. A tônica com a terça abaixada. */
    MINOR("m", listOf(0, 3, 7)),

    /** Dominante com sétima: 1, 4, 5, ♭7. */
    DOMINANT_SEVENTH("7", listOf(0, 4, 7, 10)),

    /** Maior com sétima menor: 1, 4, 5, 7. */
    MAJOR_SEVENTH("maj7", listOf(0, 4, 7, 11)),

    /** Menor com sétima: 1, ♭3, 5, ♭7. O acorde de song. */
    MINOR_SEVENTH("m7", listOf(0, 3, 7, 10)),

    /** Menor com quinta: 1, ♭3, 5, ♭7, 9. O acorde de funk. */
    MINOR_NINTH("m9", listOf(0, 3, 7, 10, 14)),

    /** Suspendido 2: tira a terça e põe a 2ª. */
    SUS2("sus2", listOf(0, 2, 7)),

    /** Suspendido 4: tira a terceira e põe a 4ª. */
    SUS4("sus4", listOf(0, 5, 7)),

    /** Diminuto: 1, ♭3, ♭5. */
    DIMINISHED("dim", listOf(0, 3, 6)),

    /** Aumentado: 1, 4, ♯5. */
    AUGMENTED("aug", listOf(0, 4, 8)),

    /** Meia diminuto: 1, ♭3, ♭5, ♭7. */
    HALF_DIMINISHED("m7b5", listOf(0, 3, 6, 10));

    /**
     * Deslocamentos em semitons a partir da tônica, normalizados para 0 a 11.
     *
     * São **relativos**, e é aqui que mora a armadilha. A tônica entra sempre
     * como 0, mas a nota que ela realmente representa tem uma classe de altura
     * própria: em Si menor, o offset 0 é a nota Si, de classe 11 — não 0.
     *
     * Comparar a nota absoluta contra estes números rejeitaria a própria
     * tônica de qualquer acorde que não seja de Dó, e o buscador devolveria
     * shapes errados ou nenhum. Por isso [contains] sempre converte para
     * intervalo a partir da raiz.
     */
    val pitchClasses: Set<Int> get() = semitones.map { ((it % 12) + 12) % 12 }.toSet()

    /**
     * A nota [midi] pertence a este acorde com tônica [rootMidi]?
     *
     * Converte a nota em **intervalo** a partir da raiz antes de comparar.
     */
    fun contains(rootMidi: Int, midi: Int): Boolean {
        val intervalo = (((midi - rootMidi) % 12) + 12) % 12
        return pitchClasses.contains(intervalo)
    }

    companion object {
        /** Reconhece um sufixo escrito, ex.: "m7" -> MINOR_SEVENTH. */
        fun fromSuffix(suffix: String): ChordQuality? =
            entries.firstOrNull { it.suffix == suffix.lowercase() }
    }
}

/** Pedido de acorde: uma tônica e uma qualidade. */
data class ChordSpec(
    val rootMidi: Int,
    val quality: ChordQuality,
) {
    val rootName: String get() = MusicTheory.labelOf(rootMidi).dropLastWhile { it.isDigit() }

    /** "C", "Am", "F#m7". */
    val name: String get() = rootName + quality.suffix
}

/**
 * Uma forma de tocar um acorde: uma casa (ou corda mudada) por corda.
 *
 * @param frets índice = número da corda. `null` = corda mudada.
 */
data class ChordShape(
    val name: String,
    val frets: List<Int?>,
    val notes: List<Int>,
    val bassMidi: Int,
    val isBarre: Boolean,
) {
    /** Menor casa presente, com a aberta valendo como 0. */
    val firstFret: Int get() = frets.filterNotNull().minOrNull() ?: 0

    /** Maior casa presente. */
    val lastFret: Int get() = frets.filterNotNull().maxOrNull() ?: 0

    /** Amplitude da pegada. 5 é o limite para a mão alcançar com um batente. */
    val span: Int get() = lastFret - firstFret

    /** Quantas casas o jogador precisa baixar para achatar a forma. */
    val barres: Int
        get() {
            val casas = frets.filterNotNull().distinct().sorted()
            if (casas.size < 2) return 0
            // Uma casa repetida em 3 ou mais cordas vira batente.
            return casas.count { casa -> frets.count { it == casa } >= 3 }
        }

    /** A forma cabe na mão? */
    val isPlayable: Boolean get() = span <= MAX_SPAN
}

/**
 * Localizador de formas de acorde.
 *
 * ## O problema
 * Um acorde como Dm no violão em afinação padrão tem dezenas de formas possíveis.
 * Só uma minoria é **tocável**: as notas precisam ser as do acorde, a pegada
 * precisa caber na mão e o baixo tem que ser a tônica (ou pelo menos uma nota
 * do acorde).
 *
 * ## A estratégia: janela deslizante
 * Em vez de testar as 16^6 combinações de casas possíveis, o algoritmo percorre
 * **faixas** de 5 casas. A justificativa é do instrumento: um jogador não
 * consegue alcançar uma casa 8 acima da menor, então toda forma tocável cabe
 * em alguma janela de 5. Reduz o espaço de busca de 16 milhões para algumas
 * centenas de combinações por janela.
 *
 * ## Por que a afinação com capo importa
 * A busca parte de [TuningPreset.midiNotes]. Com capo, quem entra ali é a
 * afinação efetiva (fase 4), e as formas saem automaticamente certainas para
 * a nota que o músico realmente ouve. Não existe passo de transposição manual.
 */
object ChordFinder {

    /** Amplitude máxima de uma pegada. 5 casas cabem na mão com batente. */
    const val MAX_SPAN = 4

    const val DEFAULT_MAX_FRET = 15

    /** Quantas formas devolver, no máximo, para a lista da tela. */
    const val MAX_RESULTS = 12

    /**
     * Encontra formas tocáveis de um acorde.
     *
     * A ordem de retorno é da mais simples para a mais complexa: menor número de
     * dedos, depois menor amplitude, depois menor casa. É a ordem em que o
     * jogador escolhe quando está com pressa.
     */
    fun findShapes(
        tuning: TuningPreset,
        spec: ChordSpec,
        maxFret: Int = DEFAULT_MAX_FRET,
        maxSpan: Int = MAX_SPAN,
    ): List<ChordShape> {
        val resultados = mutableListOf<ChordShape>()

        for (janelaInicial in 0..maxFret) {
            val janelaFinal = janelaInicial + maxSpan
            if (janelaFinal > maxFret && janelaInicial > 0) break

            // Candidatos por corda: cada casa da janela que produz nota do
            // acorde, mais a opção de mudar a corda.
            val candidatos = tuning.midiNotes.map { aberta ->
                val opcoes = mutableListOf<Int?>(null)
                for (casa in janelaInicial..janelaFinal) {
                    val nota = aberta + casa
                    if (nota <= 127 && spec.quality.contains(spec.rootMidi, nota)) opcoes += casa
                }
                opcoes
            }

            // Nenhuma corda pode ficar sem opção: uma forma que deixa uma corda
            // muda é outra forma, e será gera em outra janela.
            if (candidatos.any { it.isEmpty() }) continue

            combinar(candidatos, tuning, spec, resultados)
        }

        return resultados
            .distinctBy { it.frets }
            .sortedWith(
                compareBy(
                    { it.frets.count { f -> f != null } },       // menos dedos
                    { it.span },                                 // pegada menor
                    { it.firstFret },                            // mais perto do cravelha
                    { it.barres },                               // sem batente
                )
            )
            .take(MAX_RESULTS)
    }

    /**
     * Combina os candidatos de cada corda.
     *
     * Corta assim que encontra uma forma válida: dentro de uma janela, a
     * primeira combinação que fecha é a de menor casa em cada corda, que é
     * exatamente a que o jogador quer ver.
     */
    private fun combinar(
        candidatos: List<List<Int?>>,
        tuning: TuningPreset,
        spec: ChordSpec,
        saida: MutableList<ChordShape>,
    ) {
        val resultado = ArrayList<Int?>(candidatos.size)

        fun recurse(corda: Int) {
            if (corda == candidatos.size) {
                val notas = ArrayList<Int>(candidatos.size)
                tuning.midiNotes.forEachIndexed { i, aberta ->
                    resultado[i]?.let { notas += aberta + it }
                }
                if (notas.isEmpty()) return

                // O baixo tem que ser uma nota do acorde; senão a "fundamental"
                // é uma nota estranha que ninguém consegue nomear.
                val baixo = notas.min()
                if (!spec.quality.contains(spec.rootMidi, baixo)) return

                // Toda nota tocada tem que pertencer ao acorde.
                val distintas = notas.distinct().sorted()
                if (distintas.any { !spec.quality.contains(spec.rootMidi, it) }) return

                saida += ChordShape(
                    name = spec.name,
                    frets = resultado.toList(),
                    notes = distintas,
                    bassMidi = baixo,
                    isBarre = resultado.filterNotNull()
                        .groupingBy { it }.eachCount()
                        .any { it.value >= 3 },
                )
                return
            }

            candidatos[corda].forEach { casa ->
                resultado += casa
                recurse(corda + 1)
                resultado.removeAt(resultado.size - 1)
            }
        }

        recurse(0)
    }

    /**
     * Transpõe uma forma para outra tônica, movendo as casas no braço.
     *
     * Só funciona quando a transposição cabe no braço: um acorde 5 casas acima
     * no traste 12 sairia do instrumento. Devolve null nesse caso, e a interface
     * usa a busca do zero em vez de mostrar uma forma impossível.
     */
    fun transpose(
        tuning: TuningPreset,
        shape: ChordShape,
        semitones: Int,
        maxFret: Int = DEFAULT_MAX_FRET,
    ): ChordShape? {
        val notasPorCorda = ArrayList<Int?>(shape.frets.size)
        val novasCasas = shape.frets.map { casa ->
            when {
                casa == null -> null
                else -> {
                    val nota = tuning.midiNotes.first() + casa + semitones
                    if (nota > 127) return null
                    nota - tuning.midiNotes.first()
                }
            }
        }
        if (novasCasas.any { it != null && it > maxFret }) return null

        tuning.midiNotes.forEachIndexed { i, aberta ->
            novasCasas[i]?.let { notasPorCorda += aberta + it }
        }
        if (notasPorCorda.isEmpty()) return null

        return ChordShape(
            name = shape.name,
            frets = novasCasas,
            notes = notasPorCorda.distinct().sorted(),
            // A fundamental é a menor nota, não a da primeira corda: o baixo
            // real do acorde depende da corda mais grave que estiver soando.
            bassMidi = notasPorCorda.min(),
            isBarre = novasCasas.filterNotNull().groupingBy { it }.eachCount()
                .any { it.value >= 3 },
        )
    }

    /** As 12 tônicas, para a lista de acordes. */
    fun roots(): List<Int> = (60..71).toList()

    /** Acordes mais usados, para a lista inicial. */
    fun popularSpecs(): List<ChordSpec> =
        listOf("C", "A", "G", "E", "D", "Am", "Em", "Dm", "F", "B7")
            .mapNotNull { ChordNames.parse(it) }
}