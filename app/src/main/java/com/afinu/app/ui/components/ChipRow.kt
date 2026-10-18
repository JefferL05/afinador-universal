package com.afinu.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Fila de chips que rola horizontalmente.
 *
 * ## Por que rolagem horizontal e não menu suspenso
 * O catálogo tem 34 instrumentos. Um menu com 34 itens exige rolar uma lista
 * longa antes de ler o nome, enquanto uma fita de chips mostra 4 a 5 por vez e
 * deixa o usuário varrer os nomes com o polegar. O item selecionado aparece
 * primeiro: trocar de instrumento é raro, e rolar até o item atual é o que torna
 * a lista longazegamente inútil.
 *
 * A ordem de itens é responsabilidade do chamador — normalmente colocando o
 * selecionado no início.
 */
@Composable
fun <T> ChipRow(
    items: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    descriptionOf: ((T) -> String)? = null,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            val label = labelOf(item)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(item) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(),
                modifier = Modifier.semantics {
                    contentDescription = descriptionOf?.invoke(item) ?: label
                },
            )
        }
    }
}

/**
 * Coloca [selected] no início da lista.
 *
 * Ver [ChipRow] para o motivo. Devolve uma **cópia** — nunca reordena a lista de
 * origem, que costuma ser um catálogo compartilhado e imutável.
 */
fun <T> List<T>.movingSelectedToFront(selected: T): List<T> {
    val primeiro = firstOrNull { it == selected } ?: return this
    if (first() == primeiro) return this
    return buildList(size) {
        add(primeiro)
        forEach { if (it != primeiro) add(it) }
    }
}

/** Tamanho padrão de ícone dentro de botões com texto. */
val IconButtonIconSize = 18.dp

@Composable
fun Modifier.standardIconSize(): Modifier = this.size(IconButtonIconSize)