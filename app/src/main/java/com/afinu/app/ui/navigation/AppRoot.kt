package com.afinu.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.feature.onboarding.OnboardingScreen
import com.afinu.app.feature.onboarding.OnboardingViewModel

/**
 * Raiz da aplicação: introdução ou app.
 *
 * ## Por que a decisão fica aqui, e não dentro do NavHost
 * O `NavHost` tem um destino inicial fixo, e fazer a introdução ser um destino
 * dele significa que o botão "voltar" do Android voltaria para as páginas da
 * introdução depois do app já estar em uso. Trocar a raiz inteira evita ter que
 * limpar o back stack: quando `finished` vira verdadeiro, a introdução é
 * **substituída**, não empilhada.
 *
 * ## Por que as telas ficam montadas só depois da decisão
 * Enquanto `finished` é nulo, o app ainda não sabe se precisa do microfone.
 * Montar o afinador antes da hora forçaria uma segunda decisão de permissão
 * logo depois, e o usuário veria a tela piscar.
 */
@Composable
fun AppRoot(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
) {
    val onboarding by onboardingViewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (onboarding.finished) {
            // Ainda não sabemos. Tela neutra em vez de um salto delayout.
            null -> Box(Modifier.fillMaxSize())

            false -> OnboardingScreen(
                onFinish = onboardingViewModel::finish,
                onRequestPermission = onRequestPermission,
            )

            true -> MainScreen(onRequestPermission = onRequestPermission)
        }
    }
}