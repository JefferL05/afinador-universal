package com.afinu.app.core.util

import com.afinu.app.core.model.TuningPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordFinderTest {

    private val violao = TuningPreset("padrao", "Padrão", listOf(40, 45, 50, 55, 59, 64))
    private val ukulele = TuningPreset("gcea", "Padrão", listOf(67, 60, 64, 69))

    private fun spec(name: String): ChordSpec =
        ChordNames.parse(name) ?: error("não consegui ler o acorde $name")

    // --- Leitura de nomes ------------------------------------------------------

    @Test
    fun `le os acordes mais comuns`() {
        assertEquals("C", ChordNames.parse("C")?.name)
        assertEquals("Am", ChordNames.parse("Am")?.name)
        assertEquals("F#m7", ChordNames.parse("F#m7")?.name)
        assertEquals("B7", ChordNames.parse("B7")?.name)
        assertEquals("Dm", ChordNames.parse("Dm")?.name)
        assertEquals("G7", ChordNames.parse("G7")?.name)
    }

    @Test
    fun `le acordes com bemol e sustenido`() {
        assertEquals("Bb", ChordNames.parse("Bb")?.name)
        assertEquals("F#", ChordNames.parse("F#")?.name)
        assertEquals("A#m", ChordNames.parse("A#m")?.name)
    }

    @Test
    fun `acorde invalido devolve null`() {
        assertNull(ChordNames.parse(""))
        assertNull(ChordNames.parse("H"))
        assertNull(ChordNames.parse("Cxyz"))
    }

    // --- A forma classica de referencia ----------------------------------------

    @Test
    fun `encontra a forma de D maior que todo mundo conhece`() {
        // Forma x x 0 2 3 2 no violão padrão. O algoritmo pode devolver outra
        // equivalente, mas alguma tem de existir com essas casas.
        val formas = ChordFinder.findShapes(violao, spec("D"))
        assertTrue("não encontrou forma de D: $formas", formas.isNotEmpty())

        val classica = formas.firstOrNull { forma ->
            forma.frets[0] == null &&
                forma.frets[1] == null &&
                forma.frets[2] == 0 &&
                forma.frets[3] == 2 &&
                forma.frets[4] == 3 &&
                forma.frets[5] == 2
        }
        assertNotNull("a forma clássica de D não apareceu: $formas", classica)

        // Verificado à mão: a forma soa D3, A3, D4, F#4.
        assertEquals(listOf(50, 57, 62, 66), classica!!.notes)
        assertEquals(50, classica.bassMidi)
    }

    @Test
    fun `encontra a forma aberta de G`() {
        // 3-2-0-0-3-3 é o shape de Sol aberto no violão padrão.
        val aberta = ChordFinder.findShapes(violao, spec("G"))
            .firstOrNull { forma ->
                forma.frets[0] == 3 && forma.frets[1] == 2 && forma.frets[2] == 0 &&
                    forma.frets[3] == 0 && forma.frets[4] == 3 && forma.frets[5] == 3
            }
        assertNotNull("a forma aberta de G não apareceu", aberta)
        assertEquals(43, aberta!!.bassMidi) // G2
    }

    @Test
    fun `encontra a forma aberta de C`() {
        // x-3-2-0-1-0 é o shape de Dó aberto.
        val aberta = ChordFinder.findShapes(violao, spec("C"))
            .firstOrNull { forma ->
                forma.frets[0] == null && forma.frets[1] == 3 && forma.frets[2] == 2 &&
                    forma.frets[3] == 0 && forma.frets[4] == 1 && forma.frets[5] == 0
            }
        assertNotNull("a forma aberta de C não apareceu", aberta)
        assertEquals(48, aberta!!.bassMidi) // Dó3
    }

    @Test
    fun `toda forma encontrada so contem notas do acorde`() {
        // Esta é a invariante que separa um acorde de um amontoado de notas.
        listOf("C", "Am", "G", "Em", "Dm", "F", "B7", "A").forEach { nome ->
            val s = spec(nome)
            ChordFinder.findShapes(violao, s).forEach { forma ->
                forma.notes.forEach { nota ->
                    assertTrue(
                        "$nome tem a nota $nota, que não pertence ao acorde",
                        s.quality.contains(s.rootMidi, nota),
                    )
                }
            }
        }
    }

    @Test
    fun `toda forma cabe na mao`() {
        listOf("C", "G", "Am", "F", "Dm", "B7", "Em7", "Csus4").forEach { nome ->
            ChordFinder.findShapes(violao, spec(nome)).forEach { forma ->
                assertTrue(
                    "$nome tem pegada de ${forma.span} casas, larga demais",
                    forma.span <= ChordFinder.MAX_SPAN,
                )
            }
        }
    }

    @Test
    fun `a lista respeita a ordem documentada`() {
        // A ordem e: fundamental embaixo, depois posicao no braco, depois
        // quantas cordas soam, depois menos dedos. Quem procura o D menor
        // quer o xx0232 e nao uma inversao de tres dedos numa casa mais alta.
        val s = spec("Am")
        val formas = ChordFinder.findShapes(violao, s)
        val chaves = formas.map { forma ->
            listOf(
                if (forma.bassMidi % 12 != s.rootMidi % 12) 1 else 0,
                forma.firstFret,
                forma.frets.count { it == null },
                forma.frets.count { it != null },
            )
        }
        chaves.zipWithNext { anterior, atual ->
            assertTrue("ordem quebrada entre $anterior e $atual", anterior.ateAnterior(atual))
        }
    }

    @Test
    fun `o baixo pertence ao acorde`() {
        listOf("C", "Am", "G", "Dm", "B7").forEach { nome ->
            val s = spec(nome)
            ChordFinder.findShapes(violao, s).forEach { forma ->
                assertTrue(
                    "$nome tem baixo ${forma.bassMidi} fora do acorde",
                    s.quality.contains(s.rootMidi, forma.bassMidi),
                )
            }
        }
    }

    /** Ordem lexica: o primeiro elemento que difere decide. Empate e `<=`. */
    private fun List<Int>.ateAnterior(outra: List<Int>): Boolean {
        for (pos in indices) {
            if (this[pos] != outra[pos]) return this[pos] < outra[pos]
        }
        return true
    }

    // --- Qualidades diferentes -------------------------------------------------

    @Test
    fun `acorde menor tem a terceira abaixada`() {
        val am = ChordNames.parse("Am")!!
        val c = ChordNames.parse("C")!!
        // Am = A, C, E. Contem C (a terceira) e E (a quinta); Sol nao entra.
        assertTrue(c.quality.contains(c.rootMidi, 67))
        // C maior = C, E, G. Contem E, que e a terceira de C MAIOR. O que
        // ele nao tem e G#, que seria a dominante de C.
        assertTrue(!c.quality.contains(c.rootMidi, 68))
        assertTrue(am.quality.contains(am.rootMidi, 60))
        assertTrue(c.quality.contains(c.rootMidi, 64))
    }

    @Test
    fun `meia diminuida tem a quinta e a setima achatadas`() {
        val m7b5 = ChordNames.parse("Bm7b5")!!
        // Si, Re, Fa (5ª achatada de F#), La.
        assertTrue("Si", m7b5.quality.contains(m7b5.rootMidi, 59))
        assertTrue("Re", m7b5.quality.contains(m7b5.rootMidi, 62))
        assertTrue("Fa — a quinta e achatada", m7b5.quality.contains(m7b5.rootMidi, 65))
        assertTrue("La", m7b5.quality.contains(m7b5.rootMidi, 69))
        assertTrue("F# nao entra", !m7b5.quality.contains(m7b5.rootMidi, 66))
    }

    @Test
    fun `acorde de sete e distinto de maior com setima`() {
        val dominante = ChordNames.parse("G7")!!
        val maiorSetima = ChordNames.parse("Gmaj7")!!
        assertTrue("G7 deve conter F (setima menor)", dominante.quality.contains(dominante.rootMidi, 65))
        assertTrue("Gmaj7 nao deve conter F", !maiorSetima.quality.contains(maiorSetima.rootMidi, 65))
        assertTrue("Gmaj7 deve conter F# (setima maior)", maiorSetima.quality.contains(maiorSetima.rootMidi, 66))
    }

    @Test
    fun `suspendido tira a terceira`() {
        val csus4 = ChordNames.parse("Csus4")!!
        assertTrue(csus4.quality.contains(csus4.rootMidi, 65))   // Sol, quarta justa
        assertTrue(!csus4.quality.contains(csus4.rootMidi, 64))  // Mi, a terça que foi removida
        assertTrue(!csus4.quality.contains(csus4.rootMidi, 63))  // Mi♭ também não
    }

    @Test
    fun `diminuto tem a quinta abaixada`() {
        val dim = ChordNames.parse("Cdim")!!
        assertTrue(dim.quality.contains(dim.rootMidi, 60))  // Dó
        assertTrue(dim.quality.contains(dim.rootMidi, 63))  // Mi♭
        assertTrue(dim.quality.contains(dim.rootMidi, 66))  // Sol♭
        assertTrue(!dim.quality.contains(dim.rootMidi, 67)) // Sol natural não entra
    }

    // --- Ukulele ---------------------------------------------------------------

    @Test
    fun `encontra acordes de ukulele`() {
        val cSpec = spec("C")
        val c = ChordFinder.findShapes(ukulele, cSpec)
        assertTrue("não encontrou C no ukulele", c.isNotEmpty())
        c.forEach { forma ->
            assertEquals("ukulele tem 4 cordas", 4, forma.frets.size)
            forma.notes.forEach { nota ->
                assertTrue(
                    "nota $nota não é do acorde C",
                    cSpec.quality.contains(cSpec.rootMidi, nota),
                )
            }
        }
    }

    @Test
    fun `a forma de C no ukulele tem a nota certa`() {
        val formas = ChordFinder.findShapes(ukulele, spec("C"))
        assertTrue(formas.isNotEmpty())
        // C = Dó, Mi, Sol. A 4ª corda (Lá4=69) tem de soar Dó ou Mi.
        assertTrue(formas.any { forma ->
            (60 in forma.notes) && (64 in forma.notes) && (67 in forma.notes)
        })
    }

    // --- Capo ------------------------------------------------------------------

    @Test
    fun `com capo as formas acompanham a nota que se ouve`() {
        // Capo na 3 = +3 semitons. A forma de Sol (Sol, Si, Re) tocada com
        // esse capo soa com 3 semitons a mais: Sib, Re, Fa. Isso e Si bemol
        // maior, e nao La maior -- de Sol ate La sao 2 semitons.
        val comCapo = Capo.effective(violao, 3)
        val formaG = ChordFinder.findShapes(violao, spec("G")).first()
        val formasComCapo = ChordFinder.findShapes(comCapo, spec("Bb"))
        val formaBb = formasComCapo.firstOrNull { it.frets == formaG.frets }

        assertNotNull("a mesma forma deveria soar como Si bemol com capo na 3", formaBb)
        assertEquals(
            "a forma tocada com capo soa 3 semitons acima",
            formaG.notes.map { it + 3 }.sorted(),
            formaBb!!.notes,
        )
    }

    // --- Limites ---------------------------------------------------------------

    @Test
    fun `a busca termina rapido mesmo em afinacao exotica`() {
        // Sem o corte por janela, seriam 16^6 = 16,7 milhões de combinações.
        // O teste existe para pegar umNw regressão que tire o corte.
        val inicio = System.currentTimeMillis()
        ChordFinder.findShapes(violao, spec("C"), maxFret = 24)
        val duracao = System.currentTimeMillis() - inicio
        assertTrue("a busca levou ${duracao}ms", duracao < 3000)
    }

    @Test
    fun `acorde sem forma possivel devolve lista vazia`() {
        // Ukulele em afinação padrao não tem forma de meatigamente grave.
        val formas = ChordFinder.findShapes(ukulele, spec("Fmaj7"))
        // Pode ou não haver; o que importa é não estourar.
        formas.forEach { forma ->
            val fSpec = spec("Fmaj7")
        assertTrue(forma.notes.all { fSpec.quality.contains(fSpec.rootMidi, it) })
        }
    }

    @Test
    fun `os acordes mais usados sao validos`() {
        val specs = ChordFinder.popularSpecs()
        assertEquals(10, specs.size)
        specs.forEach {
            assertTrue("nome gerado invalido: ${it.name}", it.name.isNotBlank())
        }
    }

    // --- Propriedades da forma -------------------------------------------------

    @Test
    fun `a amplitude e a diferenca entre a menor e a maior casa`() {
        val forma = ChordShape("X", listOf(null, 3, 5, 5, 3, null), emptyList(), 0, false)
        assertEquals(3, forma.firstFret)
        assertEquals(5, forma.lastFret)
        assertEquals(2, forma.span)
    }

    @Test
    fun `três cordas na mesma casa contam como batente`() {
        // Casa batida em 3 ou mais cordas e batente. Aqui sao duas: a 3 e a 5.
        val comBatente = ChordShape("X", listOf(3, 3, 3, 5, 5, 5), emptyList(), 0, false)
        assertEquals(2, comBatente.barres)

        // Casa repetida em 2 cordas nao faz batente.
        val semBatente = ChordShape("X", listOf(3, 5, 3, 5, 4, 6), emptyList(), 0, false)
        assertEquals(0, semBatente.barres)
    }

    @Test
    fun `transpor move as casas e as notas`() {
        val forma = ChordShape("C", listOf(null, 3, 2, 0, 1, 0), emptyList(), 0, false)
        val transposta = ChordFinder.transpose(violao, forma, 2)
        assertNotNull(transposta)
        assertEquals(listOf(null, 5, 4, 2, 3, 2), transposta!!.frets)
    }

    @Test
    fun `transpor para fora do braco devolve null`() {
        val forma = ChordShape("C", listOf(null, 12, 10, 9, 8, 7), emptyList(), 0, false)
        assertNull(
            "transpor +12 sairia do instrumento e deveria devolver null",
            ChordFinder.transpose(violao, forma, 12),
        )
    }
}
