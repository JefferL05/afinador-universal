package com.afinu.app.data.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Formato do arquivo de favoritos compartilhado entre aparelhos.
 *
 * ## Por que JSON e não Room/binário
 * O usuário vai colar esse texto num chat ou num e-mail. JSON é auditável e
 * sobrevive a uma mudança de esquema do banco; o export importa os registros de
 * um app com versão diferente.
 *
 * ## Três decisiones de robustez
 * - [formatVersion] permite migrar o arquivo no futuro sem adivinhar.
 * - [ignoreUnknownKeys] faz um arquivo vindo de uma versão mais nova abrir em
 *   uma mais antiga, em vez de falhar inteiro.
 * - A versão do formato é um inteiro separado de [appVersion]: mudar o app não
 *   deveria invalidar o arquivo.
 */
@Serializable
data class FavoritesExport(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val appVersion: String = "1.0.0",
    val exportedAtEpochMs: Long = 0L,
    val items: List<Item> = emptyList(),
) {
    @Serializable
    data class Item(
        val name: String,
        val instrumentId: String,
        val tuningId: String,
        val capoFret: Int = 0,
    )

    companion object {
        const val CURRENT_FORMAT_VERSION = 1

        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun encode(items: List<Item>, appVersion: String, exportedAtEpochMs: Long): String =
            json.encodeToString(
                FavoritesExport.serializer(),
                FavoritesExport(
                    appVersion = appVersion,
                    exportedAtEpochMs = exportedAtEpochMs,
                    items = items,
                ),
            )

        /** @throws SerializationException se o texto não for um export válido. */
        fun decode(text: String): FavoritesExport =
            json.decodeFromString(FavoritesExport.serializer(), text)
    }
}

/**
 * Converte entre o domínio e o formato de exportação.
 *
 * Fica separado do repositório porque é lógica pura e vai ser testada sem Room.
 */
object FavoritesCodec {

    fun toExport(
        favorites: List<FavoriteTuning>,
        appVersion: String,
        exportedAtEpochMs: Long,
    ): String = FavoritesExport.encode(
        items = favorites.map {
            FavoritesExport.Item(
                name = it.name,
                instrumentId = it.instrumentId,
                tuningId = it.tuningId,
                capoFret = it.capoFret,
            )
        },
        appVersion = appVersion,
        exportedAtEpochMs = exportedAtEpochMs,
    )

    /**
     * Lê um export. Itens com nome vazio ou identificadores em branco são
     * descartados na leitura — um arquivo editado à mão não deve travar a
     * importação inteira.
     */
    fun fromExport(text: String): List<FavoritesExport.Item> =
        FavoritesExport.decode(text).items.filter {
            it.name.isNotBlank() && it.instrumentId.isNotBlank() && it.tuningId.isNotBlank()
        }
}