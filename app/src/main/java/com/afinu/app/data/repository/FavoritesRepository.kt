package com.afinu.app.data.repository

import com.afinu.app.core.model.Instrument
import com.afinu.app.core.model.TuningPreset
import com.afinu.app.core.util.Capo
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.data.catalog.InstrumentCatalog
import com.afinu.app.data.db.FavoriteTuningDao
import com.afinu.app.data.db.FavoriteTuningEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Um favorito visto pelo app: instrumento, afinação e casa de capo já
 * resolvidos para objetos de domínio.
 *
 * [stale] avisa quando o favorito aponta para um instrumento ou afinação que
 * não existem mais no catálogo. Isso acontece quando o app é atualizado e a
 * afinação foi renomeada. O app mostra o item em cinza com um aviso, em vez de
 * sumir com ele — o usuário pode ter digitado o nome e quer reconhecer.
 */
data class FavoriteTuning(
    val id: Long,
    val name: String,
    val instrumentId: String,
    val tuningId: String,
    val capoFret: Int,
    val instrument: Instrument?,
    val tuning: TuningPreset?,
    val stale: Boolean,
) {
    val effectiveTuning: TuningPreset?
        get() = tuning?.let { Capo.effectiveOrUnshifted(it, capoFret) }

    /** "Violão · Padrão · capo 3", ou null quando não dá para resolver. */
    val subtitle: String?
        get() {
            val inst = instrument ?: return null
            val tun = tuning ?: return null
            val capo = if (Capo.isInUse(capoFret)) " · capo $capoFret" else ""
            return "${inst.displayName} · ${tun.label}$capo"
        }

    /** Texto de uma linha para a nota mais grave com o capo aplicado. */
    fun openNoteLabel(): String? {
        val tun = tuning ?: return null
        return MusicTheory.labelOf(tun.midiNotes.min())
    }
}

/**
 * Repositório de afinações favoritas.
 *
 * Guarda identificadores, nunca notas — ver [FavoriteTuningEntity].
 */
@Singleton
class FavoritesRepository @Inject constructor(
    private val dao: FavoriteTuningDao,
) {
    val favorites: Flow<List<FavoriteTuning>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun add(
        name: String,
        instrumentId: String,
        tuningId: String,
        capoFret: Int,
    ): Long = dao.insert(
        FavoriteTuningEntity(
            name = name.trim(),
            instrumentId = instrumentId,
            tuningId = tuningId,
            capoFret = Capo.clamp(capoFret),
            createdAtEpochMs = System.currentTimeMillis(),
        )
    )

    suspend fun rename(id: Long, name: String) {
        val existente = dao.findById(id) ?: return
        dao.update(existente.copy(name = name.trim()))
    }

    suspend fun delete(id: Long) {
        dao.findById(id)?.let { dao.delete(it) }
    }

    suspend fun deleteAll() = dao.deleteAll()

    private fun FavoriteTuningEntity.toDomain(): FavoriteTuning {
        val instrumento = InstrumentCatalog.byId[instrumentId]
        val afinacao = instrumento?.tunings?.firstOrNull { it.id == tuningId }
        return FavoriteTuning(
            id = id,
            name = name,
            instrumentId = instrumentId,
            tuningId = tuningId,
            capoFret = capoFret,
            instrument = instrumento,
            tuning = afinacao,
            stale = instrumento == null || afinacao == null,
        )
    }
}