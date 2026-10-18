package com.afinu.app.ui.components

import com.afinu.app.data.catalog.InstrumentCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChipRowLogicTest {

    @Test
    fun `item selecionado vai para o inicio`() {
        val lista = listOf("a", "b", "c", "d", "e")
        assertEquals(listOf("c", "a", "b", "d", "e"), lista.movingSelectedToFront("c"))
    }

    @Test
    fun `nao reordena a lista de origem`() {
        // O catálogo é um objeto compartilhado e imutável. Reordenar no lugar
        // faria a lista mudar para todo mundo que a usa.
        val original = listOf("a", "b", "c")
        val resultado = original.movingSelectedToFront("c")

        assertEquals(listOf("a", "b", "c"), original)
        assertNotSame(original, resultado)
    }

    @Test
    fun `item ja no inicio devolve a mesma instancia`() {
        val lista = listOf("a", "b", "c")
        assertEquals(lista, lista.movingSelectedToFront("a"))
    }

    @Test
    fun `item ausente devolve a lista intacta`() {
        val lista = listOf("a", "b")
        assertEquals(lista, lista.movingSelectedToFront("z"))
    }

    @Test
    fun `nao perde nem duplica itens`() {
        val lista = InstrumentCatalog.all.map { it.id }
        val escolhida = lista.last()
        val reordenada = lista.movingSelectedToFront(escolhida)

        assertEquals(lista.size, reordenada.size)
        assertEquals(lista.toSet(), reordenada.toSet())
        assertEquals(escolhida, reordenada.first())
    }

    @Test
    fun `funciona com instrumentes que se repetem de id`() {
        // Objetos de dados podem ser iguais por valor; o primeiro que casar
        // precisa ser promoted, e a lista não pode duplicar nada.
        data class Item(val id: String, val rotulo: String)
        val lista = listOf(Item("a", "A"), Item("b", "B"), Item("a", "A"))
        val resultado = lista.movingSelectedToFront(Item("a", "A"))

        assertEquals(3, resultado.size)
        assertEquals(Item("a", "A"), resultado.first())
    }

    @Test
    fun `lista vazia nao quebra`() {
        assertTrue(emptyList<String>().movingSelectedToFront("x").isEmpty())
    }
}