package com.afinu.app.core.util

/**
 * Leitura e escrita de nomes de acorde, ex.: "F#m7", "Bbmaj7", "D".
 *
 * ## Por que um parser e não só uma busca
 * O acorde transposto não é sempre um acorde conhecido: com uma afinação em
 * drop D, o que o jogador chama de "D" soa como "C". Sem um parser que
 *vgere corretamente o nome, o app acabaria mostrando "C" para um acordes
 * que o jogador está chamando de "D" — confusão que faz ele achar o app
 * está errado.
 */
object ChordNames {

    private val LETRAS = mapOf(
        'C' to 0, 'D' to 2, 'E' to 4, 'F' to 5,
        'G' to 7, 'A' to 9, 'B' to 11,
    )

    /** Converte "C", "F#", "Bb" em semitons a partir do Dó. */
    fun rootSemitones(name: String): Int? = Fretboard.semitonesFromName(name)

    /**
     * Lê um nome de acorde completo.
     *
     * Devolve null em vez de estourar: a lista de acordes cresce conforme a
     * qualidade, e um nome desconhecido não pode derrubar a tela.
     */
    fun parse(name: String): ChordSpec? {
        val limpo = name.trim()
        if (limpo.isEmpty()) return null

        val letra = limpo[0].uppercaseChar()
        val base = LETRAS[letra] ?: return null

        var indice = 1
        var alteration = 0
        // Sustenido ou bemol logo depois da letra.
        if (indice < limpo.length && (limpo[indice] == '#' || limpo[indice] == '♯')) {
            alteration = 1; indice++
        } else if (indice < limpo.length && (limpo[indice] == 'b' || limpo[indice] == '♭')) {
            alteration = -1; indice++
        }

        val sufixo = limpo.substring(indice)
        val qualidade = when {
            sufixo.isEmpty() -> ChordQuality.MAJOR
            sufixo.equals("m", ignoreCase = true) -> ChordQuality.MINOR
            else -> ChordQuality.entries.firstOrNull { it.suffix.equals(sufixo, true) }
                ?: return null
        }

        val rootMidi = 60 + ((base + alteration + 12) % 12)
        return ChordSpec(rootMidi, qualidade)
    }

    /** Só a letra da tônica, com o acidenteo: "F#". */
    fun rootLabel(semitones: Int): String {
        val tabela = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        return tabela[((semitones % 12) + 12) % 12]
    }

    /** Nome completo: "F#m7". */
    fun labelOf(spec: ChordSpec): String = rootLabel(spec.rootMidi % 12) + spec.quality.suffix
}