package com.afinu.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R

/**
 * Introdução ao primeiro uso.
 *
 * ## Três páginas, não uma parede de texto
 * O musician já sabe afinar. O que ele não sabe é o que este app faz de
 * diferente. Cada página responde a uma pergunta real — "o que é", "por que o
 * microfone", "o que mais tem" — e nada além disso.
 *
 * ## A permissão é pedida depois da explicação, nunca antes
 * Um diálogo do sistema sem contexto é a forma mais rápida de o usuário
 * negar acesso para sempre, e não há como perguntar de novo. Por isso a página
 * do microfone vem antes do pedido, e o botão diz exatamente o que acontece ao
 * tocar nele.
 *
 * ## A promessa de privacidade aparece antes da permissão
 * É o que justifica pedir o microfone. Esconder isso até depois da tela de
 * ajuste seria pedir confiança antes de dar motivo para ela.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Peso em cima, igual ao de baixo dos pontos. Com um `height(48.dp)`
        // aqui e um `weight(1f)` lá embaixo, todo o espaço que sobrava ia para
        // um lado só: o conteúdo subia para o topo e a página ficava com um
        // buraco de ~1/3 da tela entre os indicadores e os botões. Dois pesos
        // iguais centram o conteúdo no espaço que existe acima dos botões.
        Spacer(Modifier.weight(1f))

        AnimatedContent(
            targetState = state.page,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "pagina",
        ) { pagina ->
            when (pagina) {
                0 -> PageSobre()
                1 -> PageMicrofone()
                else -> PageRecursos()
            }
        }

        Spacer(Modifier.height(32.dp))

        PageDots(current = state.page, total = 3)

        Spacer(Modifier.weight(1f))

        // "Agora não" existe de propósito: quem não quer ver a introdução
        // continua indo direto ao app, e encontra o pedido de permissão com
        // contexto na própria tela de afinador.
        TextButton(onClick = viewModel::finish) {
            Text(stringResource(R.string.onboarding_skip))
        }

        Button(
            onClick = {
                when {
                    state.isLastPage -> viewModel.finish()
                    state.page == 1 -> {
                        viewModel.next()
                        onRequestPermission()
                    }
                    else -> viewModel.next()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.isLastPage) R.string.onboarding_start else R.string.onboarding_next
                ),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PageSobre() {
    Page(
        icon = Icons.Default.GraphicEq,
        title = stringResource(R.string.onboarding_title),
        body = stringResource(R.string.onboarding_body),
    )
}

@Composable
private fun PageMicrofone() {
    Page(
        icon = Icons.Default.Mic,
        title = stringResource(R.string.onboarding_mic_title),
        body = stringResource(R.string.onboarding_mic_body),
    )
}

@Composable
private fun PageRecursos() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Item(
            icon = Icons.Default.GraphicEq,
            text = stringResource(R.string.feature_tuner),
        )
        Item(icon = Icons.Default.Timer, text = stringResource(R.string.feature_metronome))
        Item(icon = Icons.Default.CloudOff, text = stringResource(R.string.feature_offline))
    }
}

@Composable
private fun Item(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Page(icon: ImageVector, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $body"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                // Decorativo: o texto ao lado já descreve tudo, e repetir a
                // informação aqui faz o TalkBack ler duas vezes.
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Indicador de páginas.
 *
 * Fica inteiro como um único nó de leitura: três bolinhas separadas fariam o
 * TalkBack anunciar "círculo, círculo, círculo", o que não diz nada. O que o
 * usuário precisa é "página 2 de 3".
 */
@Composable
private fun PageDots(current: Int, total: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clearAndSetSemantics {
                contentDescription = "Página ${current + 1} de $total"
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        repeat(total) { i ->
            val ativo = i == current
            Box(
                Modifier
                    .size(if (ativo) 10.dp else 8.dp)
                    .background(
                        if (ativo) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape,
                    ),
            )
        }
    }
}