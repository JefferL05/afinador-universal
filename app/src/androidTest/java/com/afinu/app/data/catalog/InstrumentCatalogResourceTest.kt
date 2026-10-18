package com.afinu.app.data.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Precisa rodar no dispositivo porque resolve IDs de recurso do Android, que
 * não existem em teste unitário de JVM.
 *
 * Verifica que **todo** instrumento do catálogo tem um rótulo traduzido e que
 * as traduções pt-BR / en não ficaram vazias. Sem isto, adicionar um
 * instrumento ao catálogo e esquecer a string resultaria em o app mostrar um
 * instrumento errado para quem trocou o idioma.
 */
@RunWith(AndroidJUnit4::class)
class InstrumentCatalogResourceTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun todoInstrumentoTemRecursoDeNome() {
        InstrumentCatalog.all.forEach { instrumento ->
            val resId = InstrumentCatalog.displayNameResId(instrumento.id)
            assertTrue("Sem recurso de nome para ${instrumento.id}", resId != 0)
            assertTrue(
                "Nome vazio para ${instrumento.id}",
                context.getString(resId).isNotBlank(),
            )
        }
    }

    @Test
    fun idsInvalidosNaoQuebram() {
        val resId = InstrumentCatalog.displayNameResId("inexistente")
        assertTrue(resId != 0)
        assertTrue(context.getString(resId).isNotBlank())
    }

    @Test
    fun todasAsTraducoesTemOsMesmosNomes() {
        val ids = InstrumentCatalog.all.map { it.id }
        listOf("en", "es", "pt").forEach { idioma ->
            val configuracao = android.content.res.Configuration(context.resources.configuration)
            configuracao.setLocale(java.util.Locale(idioma))
            val recursos = context.resources
            val anteriores = recursos.configuration
            recursos.updateConfiguration(configuracao, recursos.displayMetrics)

            ids.forEach { id ->
                val resId = InstrumentCatalog.displayNameResId(id)
                val nome = try {
                    recursos.getString(resId)
                } catch (e: android.content.res.Resources.NotFoundException) {
                    ""
                }
                assertTrue("$idioma: falta tradução para $id", nome.isNotBlank())
            }

            recursos.updateConfiguration(anteriores, recursos.displayMetrics)
        }
    }
}