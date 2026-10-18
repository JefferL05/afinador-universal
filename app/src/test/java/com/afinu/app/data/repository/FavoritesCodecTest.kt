package com.afinu.app.data.repository

import com.afinu.app.data.catalog.InstrumentCatalog
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de ida e volta do formato de favoritos.
 *
 * O formato é a única coisa do app que o usuário manipula fora do app — ele
 * copia o texto para um chat e cola no outro aparelho. Um formato que quebra
 * perde o trabalho de meses digitando nomes.
 */
class FavoritesCodecTest {

    private fun item(
        name: String = "Show DGC",
        instrumentId: String = "violao",
        tuningId: String = "drop_d",
        capoFret: Int = 3,
    ) = FavoritesExport.Item(name, instrumentId, tuningId, capoFret)

    @Test
    fun `ida e volta preserva todos os campos`() {
        val originais = listOf(
            item(),
            item(name = "Aula", instrumentId = "violino", tuningId = "padrao", capoFret = 0),
            item(name = "Baixo drop D", instrumentId = "baixo4", tuningId = "drop_d", capoFret = 1),
        )

        val texto = FavoritesExport.encode(originais, "1.0.0", 1_700_000_000_000)
        val lidos = FavoritesExport.decode(texto)

        assertEquals(originais.size, lidos.items.size)
        lidos.items.forEachIndexed { i, lido ->
            assertEquals(originals[i], lido)
        }
    }

    @Test
    fun `capo zero sobrevive a ida e volta`() {
        // O bug clássico de serialização: valor zero some porque o campo foi
        // tratado como ausente. "Sem capo" e "capo 0" precisam ficar iguais.
        val texto = FavoritesExport.encode(listOf(item(capoFret = 0)), "1.0.0", 0)
        assertEquals(0, FavoritesExport.decode(texto).items.single().capoFret)
    }

    @Test
    fun `lista vazia gera arquivo valido`() {
        val texto = FavoritesExport.encode(emptyList(), "1.0.0", 0)
        assertEquals(0, FavoritesExport.decode(texto).items.size)
    }

    @Test
    fun `arquivo de versao mais nova abre numa versao mais antiga`() {
        // Chave desconhecida no futuro não pode derrubar a importação inteira.
        val futuro = """
            {
              "formatVersion": 99,
              "appVersion": "9.9.9",
              "exportedAtEpochMs": 1,
              "items": [],
              "campoDoFuturo": {"a": 1}
            }
        """.trimIndent()

        val resultado = FavoritesExport.decode(futuro)
        assertEquals(99, resultado.formatVersion)
    }

    @Test
    fun `texto invalido lanca em vez de devolver lista vazia`() {
        // Retornar vazio silenciosamente faz o usuário acreditar que importou
        // quando não importou nada.
        var lancou = false
        try {
            FavoritesExport.decode("isso nao e json {{{")
        } catch (e: SerializationException) {
            lancou = true
        }
        assertTrue("texto inválido deveria lançar SerializationException", lancou)
    }

    @Test
    fun `campos obrigatorios em branco sao descartados na leitura`() {
        // Um arquivo editado à mão não deve travar a importação inteira.
        val texto = FavoritesExport.encode(
            listOf(
                item(name = "OK"),
                item(name = "   "),
                item(instrumentId = ""),
                item(tuningId = ""),
            ),
            "1.0.0", 0,
        )

        val validos = FavoritesCodec.fromExport(texto)
        assertEquals("só o item íntegro deveria sobrar", 1, validos.size)
        assertEquals("OK", validos.single().name)
    }

    @Test
    fun `os identificados exportados existem no catalogo`() {
        // Um favorito só faz sentido se o app conseguir resolver o que aponta.
        // Este teste documenta o contrato entre o formato e o catálogo.
        val itens = FavoritesCodec.fromExport(
            FavoritesExport.encode(
                listOf(
                    item(name = "Violão drop D", instrumentId = "violao", tuningId = "drop_d"),
                    item(name = "Baixo padrão", instrumentId = "baixo4", tuningId = "padrao"),
                    item(name = "Ukulele", instrumentId = "ukulele", tuningId = "gcea"),
                ),
                "1.0.0", 0,
            )
        )

        itens.forEach { item ->
            val instrumento = InstrumentCatalog.byId[item.instrumentId]
            assertNotNull("instrumento '${item.instrumentId}' não existe", instrumento)
            assertTrue(
                "afinação '${item.tuningId}' não existe em ${item.instrumentId}",
                instrumento!!.tunings.any { it.id == item.tuningId },
            )
        }
    }

    @Test
    fun `export e legivel por humanos`() {
        // O texto vai ser colado num chat; indentado e com nomes de campo é a
        // diferença entre o usuário corrigir um item e desistir.
        val texto = FavoritesExport.encode(listOf(item()), "1.0.0", 0)
        assertTrue("JSON deveria ser indentado", texto.contains("\n"))
        assertTrue(texto.contains("\"instrumentId\""))
        assertTrue(texto.contains("\"capoFret\""))
    }
}