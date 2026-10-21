package com.afinu.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.afinu.app.R

/**
 * As quatro abas do app.
 *
 * ## Por que quatro e não seis
 * A barra é a única coisa visível o tempo todo, e seis abas em 360 dp dão
 * 60 dp por aba: qualquer rótulo com duas sílabas era cortado. Quatro abas
 * dão 90 dp cada, e o nome cabe inteiro.
 *
 * A reorganização não perdeu nada: o que era aba virou seção dentro de uma das
 * quatro, e as ferramentas de afinação que eram uma tela só passaram a ser
 * parte de quem as usa.
 */
enum class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    /** Afinador cromático: qualquer nota, sem corda de referência. */
    CROMATICO(
        route = "cromatico",
        labelRes = R.string.tab_cromatico,
        icon = Icons.Default.GraphicEq,
    ),

    /** Modo violão: uma aba por corda, com o braço à vista. */
    VIOLAO(
        route = "violao",
        labelRes = R.string.tab_violao,
        icon = Icons.Default.Tune,
    ),

    /** Biblioteca de afinações, acordes e notas de referência. */
    AFINACOES(
        route = "afinacoes",
        labelRes = R.string.tab_afinacoes,
        icon = Icons.Default.LibraryMusic,
    ),

    /** Metrônomo, tom de referência e gravação. */
    FERRAMENTAS(
        route = "ferramentas",
        labelRes = R.string.tab_ferramentas,
        icon = Icons.Default.Speed,
    );

    companion object {
        /** Rota inicial: abrir o app já afina, sem passar por menu nenhum. */
        val START: Destination = VIOLAO

        fun fromRoute(route: String?): Destination =
            entries.firstOrNull { it.route == route } ?: START
    }
}
