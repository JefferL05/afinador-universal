package com.afinu.app.data.catalog

import com.afinu.app.core.model.Instrument
import com.afinu.app.core.model.InstrumentFamily
import com.afinu.app.core.model.TuningPreset

/**
 * Catálogo de instrumentos suportados.
 *
 * Todos os valores são **notas MIDI**, do grave ao agudo. MIDI 69 = A4.
 * A ordem dentro de cada lista é a ordem física das cordas, e é dela que o app
 * deriva o índice mostrado no medidor ("corda 6" do violão).
 *
 * Regra de ouro: intervalos entre cordas **não** são digitados à mão em lugar
 * nenhum. Cada afinação declara suas notas e o intervalo sai do cálculo. Digitar
 * "+5, +5, +5, +4, +5" em algum lugar é a forma mais comum de um afinador errar
 * a 4ª corda.
 */
object InstrumentCatalog {

    private fun strings(id: String, name: String, vararg tunings: TuningPreset) =
        Instrument(id, name, InstrumentFamily.STRINGS, tunings.toList())

    private fun freeform(id: String, name: String) =
        Instrument(id, name, InstrumentFamily.FREEFORM, listOf(TuningPreset("livre", "Livre", listOf(60))))

    // --- Violão / guitarra ----------------------------------------------------

    val violao = strings(
        "violao", "Violão",
        TuningPreset("padrao", "Padrão", listOf(40, 45, 50, 55, 59, 64)),
        TuningPreset("drop_d", "Drop D", listOf(38, 45, 50, 55, 59, 64)),
        TuningPreset("drop_c", "Drop C", listOf(36, 45, 50, 55, 59, 64)),
        TuningPreset("drop_b", "Drop B", listOf(35, 45, 50, 55, 59, 64)),
        TuningPreset("dadgad", "DADGAD", listOf(38, 45, 50, 55, 57, 62)),
        TuningPreset("open_g", "Open G", listOf(38, 43, 50, 55, 59, 62)),
        TuningPreset("open_d", "Open D", listOf(38, 45, 50, 54, 57, 62)),
        TuningPreset("open_e", "Open E", listOf(40, 45, 52, 56, 59, 64)),
        TuningPreset("baritono", "Barítono (Ré menor)", listOf(38, 45, 50, 53, 57, 62)),
        TuningPreset("meio_tom", "Meio tom abaixo", listOf(39, 44, 49, 54, 58, 63)),
        TuningPreset("tom_baixo", "Tom abaixo", listOf(36, 41, 46, 51, 55, 60)),
    )

    val guitarra7 = strings(
        "guitarra7", "Guitarra 7 cordas",
        TuningPreset("padrao", "Padrão", listOf(35, 40, 45, 50, 55, 59, 64)),
        TuningPreset("drop_a", "Drop A", listOf(33, 40, 45, 50, 55, 59, 64)),
        TuningPreset("alterado", "Alterado", listOf(34, 39, 44, 49, 54, 58, 63)),
    )

    val guitarra8 = strings(
        "guitarra8", "Guitarra 8 cordas",
        TuningPreset("padrao", "Padrão", listOf(30, 35, 40, 45, 50, 55, 59, 64)),
        TuningPreset("drop_f", "Drop F#", listOf(30, 35, 40, 45, 50, 55, 59, 63)),
    )

    /**
     * Violão 12 cordas.
     *
     * Cada curso tem duas cordas separadas por uma oitava **mais uma quinta**
     * (19 semitons), não por uma oitava. Modelar as 12 individualmente faria o
     * afinador exigir duas cordas em uníssono, que é fisicamente impossível.
     * A prática real é afinar os 6 cursos inferiores: a corda aguda de cada
     * curso segue por construção. Por isso a lista tem 6 entradas.
     */
    val violao12 = strings(
        "violao12", "Violão 12 cordas",
        TuningPreset("padrao", "Padrão", listOf(40, 45, 50, 55, 59, 64)),
        TuningPreset("drop_d", "Drop D", listOf(38, 45, 50, 55, 59, 64)),
    )

    // --- Contrabaixos ---------------------------------------------------------

    val baixo4 = strings(
        "baixo4", "Baixo 4 cordas",
        TuningPreset("padrao", "Padrão", listOf(28, 33, 38, 43)),
        TuningPreset("drop_d", "Drop D", listOf(26, 33, 38, 43)),
        TuningPreset("dadgad", "DADGAD", listOf(26, 33, 38, 43)),
        TuningPreset("meio_tom", "Meio tom abaixo", listOf(27, 32, 37, 42)),
        TuningPreset("tom_baixo", "Tom abaixo", listOf(26, 31, 36, 41)),
    )

    val baixo5 = strings(
        "baixo5", "Baixo 5 cordas",
        TuningPreset("padrao", "Padrão", listOf(23, 28, 33, 38, 43)),
        TuningPreset("drop_a", "Drop A", listOf(22, 28, 33, 38, 43)),
    )

    val baixo6 = strings(
        "baixo6", "Baixo 6 cordas",
        TuningPreset("padrao", "Padrão", listOf(23, 24, 28, 33, 38, 43)),
        TuningPreset("drop_c", "Drop C", listOf(22, 24, 28, 33, 38, 43)),
    )

    // --- Outros instrumentos de cordas ----------------------------------------

    /**
     * Ukulele e cavaquinho.
     *
     * A afinação é Sol-Dó-Mi-Lá, e essa é a ordem em que o acorde é tocado — o
     * que seria um erro de catálogo. A lista de um instrumento é a ordem das
     * **cordas**, do grave para o agudo: Dó4, Mi4, Sol4, Lá4.
     */
    val ukulele = strings(
        "ukulele", "Ukulele",
        TuningPreset("gcea", "Padrão (Dó Mi Sol Lá)", listOf(60, 64, 67, 69)),
        TuningPreset("dgae", "Ré-mão (Ré Sol Si Mi)", listOf(62, 67, 71, 76)),
    )

    val cavaquinho = strings(
        "cavaquinho", "Cavaquinho",
        TuningPreset("gcea", "Padrão", listOf(60, 64, 67, 69)),
        TuningPreset("dgae", "Ré-mão", listOf(62, 67, 71, 76)),
    )

    val violino = strings(
        "violino", "Violino",
        TuningPreset("padrao", "Padrão", listOf(55, 62, 69, 76)),
    )

    val viola = strings(
        "viola", "Viola",
        TuningPreset("padrao", "Padrão", listOf(48, 55, 62, 69)),
    )

    val violoncelo = strings(
        "violoncelo", "Violoncelo",
        TuningPreset("padrao", "Padrão", listOf(36, 43, 50, 57)),
        TuningPreset("meio_tom", "Meio tom abaixo", listOf(35, 42, 49, 56)),
    )

    val contrabaixo = strings(
        "contrabaixo", "Contrabaixo acústico",
        TuningPreset("padrao", "4 cordas", listOf(28, 33, 38, 43)),
        TuningPreset("padrao5", "5 cordas", listOf(23, 28, 33, 38, 43)),
    )

    /**
     * Banjo.
     *
     * Ordem física: a 5ª corda é a mais curta e tem o timbre mais agudo, então
     * ela entra **por último**. Note que duas cordas compartilham a mesma nota
     * (Ré3 e Sol3) — isso é real, não é erro de digitação.
     */
    val banjo = strings(
        "banjo", "Banjo",
        TuningPreset("5_cordas", "5 cordas (Sol aberto)", listOf(50, 55, 55, 59, 62)),
        TuningPreset("4_cordas", "4 cordas (Sol aberto)", listOf(50, 55, 55, 62)),
    )

    val bandolim = strings(
        "bandolim", "Bandolim",
        TuningPreset("padrao", "Padrão", listOf(55, 62, 69, 76)),
    )

    val mandolino = strings(
        "mandolino", "Mandolino",
        TuningPreset("padrao", "Padrão", listOf(55, 62, 69, 76)),
    )

    /**
     * Sitar — aproximação das 4 cordas principais (bom/médios). A afinação
     * completa tem 11+ cordas e varia por região; quem usa sitar geralmente
     * afina pelo método cromático.
     */
    val sitar = strings(
        "sitar", "Sitar",
        TuningPreset("aprox", "Aproximada", listOf(58, 62, 67, 72)),
    )

    val laud = strings(
        "laud", "Laúd",
        TuningPreset("padrao", "Padrão", listOf(43, 48, 53, 57, 62)),
    )

    val balalaika = strings(
        "balalaika", "Balalaika",
        TuningPreset("padrao", "Padrão", listOf(64, 64, 69, 69)),
    )

    val guitarraPortuguesa = strings(
        "guitarra_pt", "Guitarra portuguesa",
        TuningPreset("padrao", "Padrão", listOf(38, 43, 48, 53, 57, 62)),
    )

    val guzzar = strings(
        "guzzar", "Guzzar",
        TuningPreset("padrao", "Padrão", listOf(38, 43, 48, 53)),
    )

    val lapSteel = strings(
        "lap_steel", "Lap steel",
        TuningPreset("aberto", "Aberto (com pedal em E)", listOf(84, 88, 91, 93, 96, 100)),
        TuningPreset("baritono", "Baritono aberto", listOf(28, 33, 38, 43, 47, 52)),
    )

    // --- Sopros, harmônicos, teclados e percussão ------------------------------

    val flautaDoce = freeform("flauta_doce", "Flauta doce")
    val flauta = freeform("flauta", "Flauta")
    val recorder = freeform("recorder", "Recorder")
    val oboe = freeform("oboe", "Oboé")
    val clarinete = freeform("clarinete", "Clarinete")
    val saxofone = freeform("saxofone", "Saxofone")
    val trompa = freeform("trompa", "Trompa")
    val trombone = freeform("trombone", "Trombone")
    val tuba = freeform("tuba", "Tuba")
    val harmonica = freeform("harmonica", "Harmônica")
    val gaita = freeform("gaita", "Gaita")
    val piano = freeform("piano", "Piano / Teclado")
    val acordeao = freeform("acordeao", "Acordeão")
    val bateria = freeform("bateria", "Bateria / Percussão")

    /** Ordem de exibição: cordas primeiro, depois sopros e percussão. */
    val all: List<Instrument> = listOf(
        violao, guitarra7, guitarra8, violao12,
        baixo4, baixo5, baixo6, contrabaixo,
        ukulele, cavaquinho, violino, viola, violoncelo,
        banjo, bandolim, mandolino, sitar, laud, balalaika,
        guitarraPortuguesa, guzzar, lapSteel,
        flautaDoce, flauta, recorder, oboe, clarinete, saxofone,
        trompa, trombone, tuba, harmonica, gaita,
        piano, acordeao, bateria,
    )

    val byId: Map<String, Instrument> = all.associateBy { it.id }

    /** Instrumento assumido na primeira abertura do app. */
    val default: Instrument = violao

    fun find(id: String?): Instrument = byId[id] ?: default

    /** Converte os nomes internos em rótulos tradutíveis, sem duplicar a lista. */
    fun displayNameResId(id: String): Int = when (id) {
        "violao" -> com.afinu.app.R.string.instrument_violao
        "guitarra7" -> com.afinu.app.R.string.instrument_guitarra7
        "guitarra8" -> com.afinu.app.R.string.instrument_guitarra8
        "violao12" -> com.afinu.app.R.string.instrument_violao12
        "baixo4" -> com.afinu.app.R.string.instrument_baixo4
        "baixo5" -> com.afinu.app.R.string.instrument_baixo5
        "baixo6" -> com.afinu.app.R.string.instrument_baixo6
        "contrabaixo" -> com.afinu.app.R.string.instrument_contrabaixo
        "ukulele" -> com.afinu.app.R.string.instrument_ukulele
        "cavaquinho" -> com.afinu.app.R.string.instrument_cavaquinho
        "violino" -> com.afinu.app.R.string.instrument_violino
        "viola" -> com.afinu.app.R.string.instrument_viola
        "violoncelo" -> com.afinu.app.R.string.instrument_violoncelo
        "banjo" -> com.afinu.app.R.string.instrument_banjo
        "bandolim" -> com.afinu.app.R.string.instrument_bandolim
        "mandolino" -> com.afinu.app.R.string.instrument_mandolino
        "sitar" -> com.afinu.app.R.string.instrument_sitar
        "laud" -> com.afinu.app.R.string.instrument_laud
        "balalaika" -> com.afinu.app.R.string.instrument_balalaika
        "guitarra_pt" -> com.afinu.app.R.string.instrument_guitarra_pt
        "guzzar" -> com.afinu.app.R.string.instrument_guzzar
        "lap_steel" -> com.afinu.app.R.string.instrument_lap_steel
        "flauta_doce" -> com.afinu.app.R.string.instrument_flauta_doce
        "flauta" -> com.afinu.app.R.string.instrument_flauta
        "recorder" -> com.afinu.app.R.string.instrument_recorder
        "oboe" -> com.afinu.app.R.string.instrument_oboe
        "clarinete" -> com.afinu.app.R.string.instrument_clarinete
        "saxofone" -> com.afinu.app.R.string.instrument_saxofone
        "trompa" -> com.afinu.app.R.string.instrument_trompa
        "trombone" -> com.afinu.app.R.string.instrument_trombone
        "tuba" -> com.afinu.app.R.string.instrument_tuba
        "harmonica" -> com.afinu.app.R.string.instrument_harmonica
        "gaita" -> com.afinu.app.R.string.instrument_gaita
        "piano" -> com.afinu.app.R.string.instrument_piano
        "acordeao" -> com.afinu.app.R.string.instrument_acordeao
        "bateria" -> com.afinu.app.R.string.instrument_bateria
        else -> com.afinu.app.R.string.instrument_violao
    }
}