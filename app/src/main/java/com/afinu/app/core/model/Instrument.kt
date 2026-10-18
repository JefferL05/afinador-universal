package com.afinu.app.core.model

/** Família do instrumento — define como o afinador se comporta. */
enum class InstrumentFamily {
    /** Cordas com afinação definida. Ex.: violão, baixo, ukulele. */
    STRINGS,

    /** Sopro / percussão: sem afinação fixa, o app opera em modo cromático. */
    FREEFORM,
}

/**
 * Um instrumento suportado pelo afinador.
 *
 * [displayName] é o rótulo embutido em português e serve de reserva para o
 * caso o recurso de string não exista. Ver `InstrumentCatalog.displayNameResId`.
 */
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

    /** Nota mais grave possível entre todas as afinações — define a janela do detector. */
    val lowestMidi: Int get() = tunings.minOf { it.lowestMidi }

    /**
     * Instrumentos com fundamental abaixo de D3 precisam de janela de análise
     * maior: um E1 (41 Hz) tem período de 1165 amostras a 48 kHz, e a janela
     * precisa cobrir cerca de dois períodos.
     */
    val needsDeepWindow: Boolean get() = lowestMidi <= MIDI_D3
}

const val MIDI_D3 = 50