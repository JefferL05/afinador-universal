package com.afinu.app.data.catalog

import com.afinu.app.core.model.InstrumentFamily
import com.afinu.app.core.util.Capo
import com.afinu.app.core.util.MusicTheory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O catálogo é dado, não código — e dado errado aparece em silêncio. Estes
 * testes existem para que uma afinação digitada errado não chegue ao usuário.
 */
class InstrumentCatalogTest {

    @Test
    fun `nenhum id de instrumento e duplicado`() {
        val ids = InstrumentCatalog.all.map { it.id }
        assertEquals("IDs duplicados: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}",
            ids.size, ids.toSet().size)
    }

    @Test
    fun `nenhum id de afinacao e duplicado dentro de um instrumento`() {
        InstrumentCatalog.all.forEach { instrumento ->
            val ids = instrumento.tunings.map { it.id }
            assertEquals("${instrumento.id} tem afinações duplicadas: $ids",
                ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `todas as notas estao na faixa valida`() {
        InstrumentCatalog.all.forEach { instrumento ->
            instrumento.tunings.forEach { afinacao ->
                afinacao.midiNotes.forEach { midi ->
                    assertTrue("${instrumento.id}/${afinacao.id}: MIDI $midi inválido", midi in 0..127)
                }
            }
        }
    }

    @Test
    fun `todas as cordas estao em ordem crescente`() {
        InstrumentCatalog.all.forEach { instrumento ->
            instrumento.tunings.forEach { afinacao ->
                val ordenadas = afinacao.midiNotes.sorted()
                assertEquals(
                    "${instrumento.id}/${afinacao.id} não está do grave para o agudo: ${afinacao.midiNotes}",
                    ordenadas, afinacao.midiNotes,
                )
            }
        }
    }

    @Test
    fun `violao padrao tem as seis cordas corretas`() {
        val padrao = InstrumentCatalog.violao.defaultTuning
        assertEquals(listOf(40, 45, 50, 55, 59, 64), padrao.midiNotes)

        // E2 A2 D3 G3 B3 E4
        val intervalos = padrao.midiNotes.zipWithNext { a, b -> b - a }
        assertEquals(listOf(5, 5, 5, 4, 5), intervalos)
    }

    @Test
    fun `baixo padrao tem intervalos de quarta e quinta justas`() {
        val padrao = InstrumentCatalog.baixo4.defaultTuning
        assertEquals(listOf(28, 33, 38, 43), padrao.midiNotes)
        assertEquals(listOf(5, 5, 5), padrao.midiNotes.zipWithNext { a, b -> b - a })
    }

    @Test
    fun `ukulele tem quatro cordas`() {
        assertEquals(4, InstrumentCatalog.ukulele.defaultTuning.stringCount)
        assertEquals(listOf(67, 60, 64, 69), InstrumentCatalog.ukulele.defaultTuning.midiNotes.sorted())
    }

    @Test
    fun `violao de doze cordas e modelado por cursos`() {
        // As duas cordas de um curso estão separadas por 19 semitons, uma
        // oitava mais uma quinta. Modelar as 12 individualmente exigiria duas
        // cordas em uníssono, o que é fisicamente impossível.
        assertEquals(6, InstrumentCatalog.violao12.defaultTuning.stringCount)
    }

    @Test
    fun `toda afinacao aceita capo ate a decima segunda casa`() {
        // Sem capo no violão, ukulele ou baixo, a tela de ajustes produziria uma
        // afinação fora da faixa MIDI e o medidor ficaria sem alvo.
        InstrumentCatalog.all
            .filter { it.family == InstrumentFamily.STRINGS }
            .forEach { instrumento ->
                instrumento.tunings.forEach { afinacao ->
                    (Capo.MIN_FRET..Capo.MAX_FRET).forEach { fret ->
                        val efetiva = Capo.effectiveOrUnshifted(afinacao, fret)
                        assertEquals(
                            "${instrumento.id}/${afinacao.id} capo $fret perdeu cordas",
                            afinacao.stringCount,
                            efetiva.stringCount,
                        )
                        assertTrue(
                            "${instrumento.id}/${afinacao.id} capo $fret saiu da faixa",
                            efetiva.midiNotes.all { it in 0..127 },
                        )
                    }
                }
            }
    }

    @Test
    fun `toda afinacao aceita deslocamento de ate doze semitons`() {
        // Cobre o outro lado da transposicao, usado por quem quiser afinar
        // meio tom abaixo sem trocar o instrumento.
        InstrumentCatalog.all
            .filter { it.family == InstrumentFamily.STRINGS }
            .forEach { instrumento ->
                instrumento.tunings.forEach { afinacao ->
                    val abaixo = Capo.effectiveOrUnshifted(afinacao, -12)
                    assertEquals(
                        "${instrumento.id}/${afinacao.id} perdeu cordas ao descer uma oitava",
                        afinacao.stringCount,
                        abaixo.stringCount,
                    )
                }
            }
    }

    @Test
    fun `guitarra de oito cordas alcanca Fsharp1`() {
        val maisGrave = InstrumentCatalog.guitarra8.defaultTuning.midiNotes.min()
        assertEquals(30, maisGrave)
        assertEquals(61.7f, MusicTheory.frequencyOf(maisGrave, 440f), 0.5f)
    }

    @Test
    fun `instrumentos graves pedem janela profunda`() {
        assertTrue("baixo precisa de janela profunda", InstrumentCatalog.baixo5.needsDeepWindow)
        assertTrue("contrabaixo precisa de janela profunda", InstrumentCatalog.baixo4.needsDeepWindow)
        assertTrue("violoncelo precisa de janela profunda", InstrumentCatalog.violoncelo.needsDeepWindow)
        assertTrue("violao precisa de janela profunda", InstrumentCatalog.violao.needsDeepWindow)
        assertTrue("violino dispensa janela profunda", !InstrumentCatalog.violino.needsDeepWindow)
    }

    @Test
    fun `instrumentos sem afinacao fixa sao da familia livre`() {
        InstrumentCatalog.all
            .filter { it.family == InstrumentFamily.FREEFORM }
            .forEach {
                assertEquals("${it.id} não deveria ter afinação de cordas", 1, it.defaultTuning.stringCount)
            }
    }

    @Test
    fun `instrumento desconhecido cai no padrao`() {
        assertEquals(InstrumentCatalog.default, InstrumentCatalog.find("inexistente"))
        assertEquals(InstrumentCatalog.default, InstrumentCatalog.find(null))
    }
}