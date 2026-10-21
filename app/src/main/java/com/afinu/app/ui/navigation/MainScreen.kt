package com.afinu.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afinu.app.feature.chords.ChordsScreen
import com.afinu.app.feature.metronome.MetronomeScreen
import com.afinu.app.feature.notes.NotesCardScreen
import com.afinu.app.feature.recorder.RecorderScreen
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
                        label = {
                            // Uma linha, sempre. Sem isto o rótulo quebra no
                            // meio da palavra ("Afinad / or", "Metrôn / omo")
                            // porque seis abas em 360 dp cabem em ~60 dp cada.
                            // `labelSmall` devolve o espaço que o `labelMedium`
                            // padrão do Material 3 não tem, e o corte por
                            // reticências vira exceção em vez de rotina.
                            //
                            // O rótulo visível pode ser abreviado ("Metr."), mas
                            // o leitor de tela anuncia o nome inteiro:
                            // `clearAndSetSemantics` troca o texto lido pela
                            // descrição sem mexer no estado de seleção do item,
                            // que vive no nó de cima.
                            val nomeCompleto = stringResource(destino.accessibilityLabelRes)
                            Text(
                                text = stringResource(destino.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.clearAndSetSemantics {
                                    contentDescription = nomeCompleto
                                },
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        // O `Scaffold` do Material 3 já calcula os insets do sistema — barra de
        // status em cima, barra de navegação embaixo — e devolve tudo pronto em
        // `padding`. Antes só o rodapé era aplicado, então todo título desenhava
        // por baixo da barra de status: "Violão" ficava em cima do relógio e
        // "Trocar" em cima do wifi em todas as seis telas. Com targetSdk 35 o
        // edge-to-edge é imposto pelo sistema, não é uma escolha do tema.
        //
        // Nenhuma tela tem `TopAppBar` nem `Scaffold` próprio, então aplicar o
        // padding completo não duplica nada.
        NavHost(
            navController = navController,
            startDestination = Destination.START.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Destination.AFINADOR.route) {
                TunerScreen(onRequestPermission = onRequestPermission)
            }
            composable(Destination.METRONOME.route) { MetronomeScreen() }
            composable(Destination.NOTAS.route) { NotesCardScreen() }
            composable(Destination.ACORDES.route) { ChordsScreen() }
            composable(Destination.GRAVAR.route) { RecorderScreen() }
            composable(Destination.AJUSTES.route) { SettingsScreen() }
        }
    }
}