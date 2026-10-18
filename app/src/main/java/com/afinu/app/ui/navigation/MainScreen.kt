package com.afinu.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afinu.app.feature.chords.ChordsScreen
import com.afinu.app.feature.metronome.MetronomeScreen
import com.afinu.app.feature.notes.NotesCardScreen
import com.afinu.app.feature.settings.SettingsScreen
import com.afinu.app.feature.tuner.TunerScreen

/**
 * Casca da aplicação: bottom bar + conteúdo.
 *
 * ## Por que a barra fica embaixo e não em cima
 * Ela é usada com uma mão só, enquanto a outra segura o instrumento. Em cima,
 * o polegar não alcança. Em palco, olhar para baixo também é mais rápido do que
 * levantar os olhos para o topo.
 *
 * ## Por que o afinador não fica em "dentro" da navegação
 * A tela de afinação é o produto. A barra existe para as outras quatro telas
 * acessíveis *a partir dela*, e não o contrário. Por isso o grafo de navegação
 * tem o afinador como entrada e as demais como irmãs.
 */
@Composable
fun MainScreen(
    onRequestPermission: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rotaAtual = backStackEntry?.destination?.route
    val destinoAtual = Destination.fromRoute(rotaAtual)

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destino ->
                    NavigationBarItem(
                        selected = destino == destinoAtual,
                        onClick = {
                            if (destino != destinoAtual) {
                                navController.navigate(destino.route) {
                                    // Trocar de aba não deve empilhar telas:
                                    // voltar do Metrônomo tem que ir para o
                                    // Afinador, não para uma pilha infinita.
                                    popUpTo(Destination.START.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(destino.icon, contentDescription = null) },
                        label = { Text(stringResource(destino.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.START.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Destination.AFINADOR.route) {
                TunerScreen(onRequestPermission = onRequestPermission)
            }
            composable(Destination.METRONOME.route) { MetronomeScreen() }
            composable(Destination.NOTAS.route) { NotesCardScreen() }
            composable(Destination.ACORDES.route) { ChordsScreen() }
            composable(Destination.AJUSTES.route) { SettingsScreen() }
        }
    }
}