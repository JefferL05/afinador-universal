package com.afinu.app.feature.metronome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afinu.app.R

/**
 * Marcador de posição do metrônomo.
 *
 * ## Por que isto ainda não é um metrônomo
 * A tentação é usar `Thread.sleep()` ou um `Timer` de 100 ms. Os dois
 *-produzem batidas irregulares — erro acumulado de alguns milissegundos por
 * minuto — e isso é exatamente o que um metrônomo não pode ter.
 *
 * A implementação correta agenda os cliques contando **amostras** num
 * `AudioTrack`: o relógio é o próprio dispositivo de áudio, que não deriva. Um
 * `AudioTrack` contínuo com um índice de amostra acumulado por batida dá um
 * metrônomo que não sai do tempo nem com a CPU ocupada.
 *
 * Deixar a tela com um aviso honesto é melhor do que entregar um metrônomo que
 * "parece funcionar" e faz o músico acelerar junto com o defeito.
 */
@Composable
fun MetronomeScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.metronome),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.metronome_coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}