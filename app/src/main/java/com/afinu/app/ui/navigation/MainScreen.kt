package com.afinu.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afinu.app.feature.metronome.ToolsScreen
import com.afinu.app.feature.settings.SettingsScreen
import com.afinu.app.feature.settings.TuningLibraryScreen
import com.afinu.app.feature.tuner.ChromaticTunerScreen
import com.afinu.app.feature.tuner.HeadstockTunerScreen
import com.afinu.app.ui.components.TuneMasterBottomBar
import com.afinu.app.ui.components.TuneMasterHeader

/**
 * A casca do aplicativo: cabeçalho, barra de navegação e o conteúdo.
 *
 * ## Por que a barra fica embaixo e não em cima
 * Ela é usada com uma mão só, enquanto a outra segura o instrumento. Em cima,
 * o polegar não alcança. Em palco, olhar para baixo também é mais rápido do
 * que levantar os olhos para o topo.
 *
 * ## Por que o afinador não fica em "dentro" da navegação
 * A tela de afinação é o produto. A barra existe para as outras três telas
 * acessíveis *a partir dela*, e não o contrário.
 */
@Composable
fun MainScreen(
    onRequestPermission: () -> Unit,
    navController: NavHostController = rememberNavController(),
    shellViewModel: AppShellViewModel = hiltViewModel(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rotaAtual = backStackEntry?.destination?.route
    val destinoAtual = Destination.fromRoute(rotaAtual)
    val a4Hz by shellViewModel.a4Hz.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TuneMasterHeader(
                section = stringResource(destinoAtual.labelRes),
                a4Hz = a4Hz,
            )
        },
        bottomBar = {
            TuneMasterBottomBar(
                atual = destinoAtual,
                onSelect = { destino ->
                    if (destino != destinoAtual) {
                        navController.navigate(destino.route) {
                            // Trocar de aba não deve empilhar telas: voltar do
                            // Metrônomo tem que ir para o Afinador, não para uma
                            // pilha infinita.
                            popUpTo(Destination.START.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        },
    ) { padding ->
        // O `Scaffold` já calcula os insets do sistema — barra de status em
        // cima, barra de navegação embaixo — e devolve tudo pronto em
        // `padding`. Com targetSdk 35 o edge-to-edge é imposto pelo sistema.
        NavHost(
            navController = navController,
            startDestination = Destination.START.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Destination.CROMATICO.route) {
                ChromaticTunerScreen(onRequestPermission = onRequestPermission)
            }
            composable(Destination.VIOLAO.route) {
                HeadstockTunerScreen(onRequestPermission = onRequestPermission)
            }
            composable(Destination.AFINACOES.route) {
                TuningLibraryScreen(
                    onOpenSettings = {
                        navController.navigate(ROTA_AJUSTES)
                    },
                )
            }
            composable(Destination.FERRAMENTAS.route) {
                ToolsScreen()
            }
            // Os Ajustes não são uma aba: são um destravo dentro da biblioteca
            // de afinações, que é onde o capo, a referência de altura e os
            // favoritos já vivem. Uma quinta aba roubaria espaço das quatro.
            composable(ROTA_AJUSTES) {
                SettingsScreen()
            }
        }
    }
}

/** A rota dos Ajustes, alcançada pelo botão de filtro da biblioteca. */
const val ROTA_AJUSTES = "ajustes"
