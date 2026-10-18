package com.afinu.app.feature.metronome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R
import com.afinu.app.ui.accessibility.labelAndState
import com.afinu.app.ui.components.ChipRow

/**
 * Tela do metrônomo.
 *
 * ## O botão precisa ser grande e ter dois estados visíveis
 * O musician aperta o metrônomo no meio de uma frase, às vezes com o
 * instrumento no colo. Alvo pequeno e feedback ambíguo resultam em dois toques
 * — ou pior, em ficar believing que desligou quando ligou.
 *
 * ## O pulso do compasso
 * O círculo grande pisca a cada batida. É redundante com o som, e é justamente
 * por isso que funciona: quem está ensaiando num lugar barulhento não ouve o
 * metrônomo, mas vê o pulso. Também dá para afinar "no olho" com o movimento.
 */
@Composable
fun MetronomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MetronomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.metronome),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(20.dp))

        PulseIndicator(
            isRunning = state.isRunning,
            accentBeat = state.accentBeat,
            beatsPerBar = state.config.beatsPerBar,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "${state.config.bpm.toInt()}",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Thin,
        )
        Text(
            text = stringResource(R.string.bpm_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        FilledIconButton(
            onClick = viewModel::toggle,
            modifier = Modifier
                .size(88.dp)
                .clearAndSetSemantics {
                    contentDescription = stringResource(
                        if (state.isRunning) R.string.pause else R.string.start
                    )
                },
        ) {
            Icon(
                imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Slider(
            value = state.config.bpm,
            onValueChange = viewModel::setBpm,
            valueRange = MetronomeConfig.MIN_BPM..MetronomeConfig.MAX_BPM,
            // 270 passos de 1 BPM entre 30 e 300.
            steps = 268,
            modifier = Modifier.labelAndState(
                label = stringResource(R.string.bpm_label),
                value = "${state.config.bpm.toInt()} BPM",
            ),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.meter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        ChipRow(
            items = MetronomeConfig.COMMON_METERS,
            selected = state.config.beatsPerBar,
            labelOf = { "$it" },
            descriptionOf = { "$it tempos por compasso" },
            onSelect = viewModel::setBeatsPerBar,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.subdivision),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        ChipRow(
            items = SUBDIVISIONS,
            selected = state.config.subdivisionsPerBeat,
            labelOf = { it.second },
            descriptionOf = { it.second },
            onSelect = { viewModel.setSubdivisions(it.first) },
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.tempos),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetronomeConfig.PRESETS.take(4).forEach { (nome, bpm) ->
                AssistChip(
                    onClick = { viewModel.setBpm(bpm) },
                    label = { Text(nome) },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Círculo que pulsa a cada batida.
 *
 * O anel de fora mostra onde o compasso está; os pontos internos marcam cada
 * tempo, com o tempo 1 destacado. Sem os pontos, o usuário não sabe em que tempo
 * está e erra a entrada depois de uma pausa.
 */
@Composable
private fun PulseIndicator(
    isRunning: Boolean,
    accentBeat: Int,
    beatsPerBar: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(72.dp)
            .semantics {
                contentDescription = if (isRunning) {
                    "Batendo, tempo $accentBeat de $beatsPerBar"
                } else {
                    "Parado"
                }
            },
    ) {
        repeat(beatsPerBar) { indice ->
            val ativo = isRunning && indice + 1 == accentBeat
            val primeiro = indice == 0
            Box(
                Modifier
                    .size(if (primeiro) 44.dp else 36.dp)
                    .clip(CircleShape)
                    .then(
                        if (ativo) {
                            Modifier.background(MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        }
                    ),
            )
        }
    }
}

private val SUBDIVISIONS = listOf(
    1 to "Semínimas",
    2 to "Colcheias",
    4 to "Semicolcheias",
)