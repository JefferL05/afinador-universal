package com.afinu.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    // `LazyRow` e não `Row + horizontalScroll`: um `Row` com rolagem mede os
    // filhos com a largura da viewport como limite, então o `Text` encolhe e
    // quebra linha — era assim que "Moderato" virava "Mo / der / ato" no
    // metrônomo. `LazyRow` mede cada chip com largura ilimitada e o scroll
    // passa a acontecer de verdade quando não cabe.
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items) { item ->
            val isSelected = item == selected
            val label = labelOf(item)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(item) },
                label = {
                    // Uma linha, sempre. Mesma razão da barra de navegação.
                    Text(label, maxLines = 1, softWrap = false)
                },
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
    // `this` dentro de `buildList` é a lista nova, não a de origem — por isso a
    // origem fica num nome próprio antes de abrir o construtor.
    val lista = this
    val indice = lista.indexOfFirst { it == selected }
    // -1 = ausente, 0 = já no início: nos dois casos devolve a própria
    // instância, sem gastar uma cópia.
    if (indice <= 0) return lista

    // Move pelo ÍNDICE, não por igualdade: se o item se repete na lista, só a
    // ocorrência escolhida sai do lugar e as outras ficam onde estavam.
    return buildList(lista.size) {
        add(lista[indice])
        for (i in lista.indices) if (i != indice) add(lista[i])
    }
}

/** Tamanho padrão de ícone dentro de botões com texto. */
val IconButtonIconSize = 18.dp

@Composable
fun Modifier.standardIconSize(): Modifier = this.size(IconButtonIconSize)