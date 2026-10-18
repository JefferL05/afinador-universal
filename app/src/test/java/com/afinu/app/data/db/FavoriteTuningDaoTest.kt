package com.afinu.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.afinu.app.data.catalog.InstrumentCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes do banco com Robolectric.
 *
 * Rodam em JVM com um SQLite em memória: mais rápido e mais estável que subir
 * um emulador só para testar cinco queries.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FavoriteTuningDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FavoriteTuningDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.favoriteTunings()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entidade(
        name: String,
        instrumentId: String = "violao",
        tuningId: String = "padrao",
        capoFret: Int = 0,
        createdAt: Long = 0,
    ) = FavoriteTuningEntity(
        name = name,
        instrumentId = instrumentId,
        tuningId = tuningId,
        capoFret = capoFret,
        createdAtEpochMs = createdAt,
    )

    @Test
    fun `insere e le de volta`() = runTest {
        val id = dao.insert(entidade("Show"))

        val lido = dao.findById(id)
        assertEquals("Show", lido?.name)
        assertEquals("violao", lido?.instrumentId)
        assertEquals(0, lido?.capoFret)
    }

    @Test
    fun `lista vem do mais recente para o mais antigo`() = runTest {
        dao.insert(entidade("Antigo", createdAt = 1_000))
        dao.insert(entidade("Do meio", createdAt = 2_000))
        dao.insert(entidade("Recente", createdAt = 3_000))

        val nomes = dao.observeAll().first().map { it.name }
        assertEquals(listOf("Recente", "Do meio", "Antigo"), nomes)
    }

    @Test
    fun `renomeia preservando os demais campos`() = runTest {
        val id = dao.insert(entidade("Nome antigo", capoFret = 5))
        val atual = dao.findById(id)!!

        dao.update(atual.copy(name = "Nome novo"))

        val lido = dao.findById(id)!!
        assertEquals("Nome novo", lido.name)
        assertEquals("a casa do capo não pode ser perdida ao renomear", 5, lido.capoFret)
        assertEquals("violao", lido.instrumentId)
    }

    @Test
    fun `apaga apenas o alvo`() = runTest {
        val alvo = dao.insert(entidade("Alvo"))
        dao.insert(entidade("Outro"))

        dao.delete(dao.findById(alvo)!!)

        assertNull(dao.findById(alvo))
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun `fluxo reage a novas insercoes`() = runTest {
        val antes = dao.observeAll().first().size
        dao.insert(entidade("Nova"))
        assertEquals(antes + 1, dao.observeAll().first().size)
    }

    @Test
    fun `apagar tudo limpa a tabela`() = runTest {
        repeat(5) { dao.insert(entidade("Item $it", createdAt = it.toLong())) }

        dao.deleteAll()

        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun `identificadores salvos apontam para o catalogo atual`() = runTest {
        // O contrato do repositório: guardar id, não notas, para que corrigir o
        // catálogo corrija também os favoritos já salvos.
        val id = dao.insert(entidade("X", instrumentId = "violao", tuningId = "drop_d"))
        val lido = dao.findById(id)!!

        val instrumento = InstrumentCatalog.byId[lido.instrumentId]
        assertTrue("instrumento não encontrado", instrumento != null)
        assertTrue(
            "afinação não encontrada",
            instrumento!!.tunings.any { it.id == lido.tuningId },
        )
    }

    @Test
    fun `id inexistente devolve null em vez de estourar`() = runTest {
        assertNull(dao.findById(999_999))
    }
}