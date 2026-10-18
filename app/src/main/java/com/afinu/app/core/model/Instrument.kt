package com.afinu.app.core.model

/** Família do instrumento — define como o afinador se comporta. */
enum class InstrumentFamily {
    /** Cordas com afinação definida. Ex.: violão, baixo, ukulele. */
    STRINGS,

    /** Sopro / percussão: sem afinação fixa, o app opera em modo cromático. */
    FREEFORM,
}

/**
 * Uma afinação (padrão ou alternativa) de um instrumento de cordas.
 *
 * [midiNotes] vai sempre do grave ao agudo e o número de elementos é o número
 * de cordas, inclusive nas de 7, 8 e 12 cordas e no ukulele, que tem as
 * notas fora de ordem se lidas da corda mais aguda para a mais grave.
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
}

/** Um instrumento suportado pelo afinador. */
data class Instrument(
    val id: String,
    val displayName: String,
    val family: InstrumentFamily,
    val tunings: List<TuningPreset>,
) {
    init {
        require(tunings.isNotEmpty()) { "Instrumento sem nenhuma afinação: $id" }
        require(tunings.size == tunings.map { it.id }.toSet().size) {
            "IDs de afinação duplicados em $id"
        }
    }

    /** Afinação usada quando o app abre. */
    val defaultTuning: TuningPreset get() = tunings.first()

    /** Nota mais grave possível — define o tamanho da janela de análise. */
    val lowestMidi: Int get() = tunings.minOf { t -> t.midiNotes.min() }

    /**
     * Instrumentos com fundamental abaixo de D3 precisam de uma janela de
     * análise maior: a janela precisa cobrir pelo menos ~2 períodos do sinal,
     * e um E1 (41 Hz) tem período de 1165 amostras a 48 kHz.
     */
    val needsDeepWindow: Boolean get() = lowestMidi <= MIDI_D3
}

const val MIDI_D3 = 50
