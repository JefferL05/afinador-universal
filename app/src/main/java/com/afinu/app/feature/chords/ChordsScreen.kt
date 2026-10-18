package com.afinu.app.feature.chords

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
 * Marcador de posição dos acordes e do fretboard.
 *
 * ## A dependência do capo, e por que ela vem primeiro
 * Diagramas de acorde só fazem sentido depois de duas decisões: em que afinação
 * o instrumento está e em que casa o capo está. Com afinação padrão e sem capo,
 * o diagrama de D é o de sempre; com capo na 3ª casa, o mesmo desenho é o acorde
 * de F. Desenhar o diagrama antes de resolver isso gera a forma mais comum de
 * erro: mostrar um desenho bonito e errado.
 *
 * A Fase 5 monta os diagramas por transposição do fretboard a partir da
 * afinação efetiva que a Fase 4 já produziu, então a dependência já está
 * pronta.
 */
@Composable
fun ChordsScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.chords),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.chords_coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}