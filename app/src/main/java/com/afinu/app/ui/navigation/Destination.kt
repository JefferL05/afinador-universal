package com.afinu.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.afinu.app.R

/**
 * Destinos do bottom bar.
 *
 * A ordem não é alfabética: vai do que se usa o tempo todo para o que se
 * configura uma vez. O afinador fica primeiro e é o destino inicial, porque o
 * app existe para afinar.
 *
 * Cada aba é um destino **independente**, com ViewModel próprio. É o que
 * garante que trocar de aba e voltar não reinicie a captura de áudio: sair da
 * tela do afinador dispara `onCleared`, que libera o microfone na hora.
 */
enum class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    AFINADOR(
        route = "afinador",
        labelRes = R.string.tab_tuner,
        icon = Icons.Default.GraphicEq,
    ),
    METRONOME(
        route = "metronomo",
        labelRes = R.string.tab_metronome,
        icon = Icons.Default.Timer,
    ),
    NOTAS(
        route = "notas",
        labelRes = R.string.tab_notes,
        icon = Icons.Default.MusicNote,
    ),
    ACORDES(
        route = "acordes",
        labelRes = R.string.tab_chords,
        icon = Icons.Default.LibraryMusic,
    ),
    GRAVAR(
        route = "gravar",
        labelRes = R.string.tab_recorder,
        icon = Icons.Default.Mic,
    ),
    AJUSTES(
        route = "ajustes",
        labelRes = R.string.tab_settings,
        icon = Icons.Default.Settings,
    );

    companion object {
        /** Rota inicial: abrir o app já afina, sem passar por menu nenhum. */
        val START: Destination = AFINADOR

        fun fromRoute(route: String?): Destination =
            entries.firstOrNull { it.route == route } ?: START
    }
}