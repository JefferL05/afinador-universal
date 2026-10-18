package com.afinu.app.feature.tuner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R
import com.afinu.app.core.model.Instrument
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.data.catalog.InstrumentCatalog

/**
 * Tela principal do app.
 *
 * Não há menu, não há "entrar": o app abre aqui. O único caminho entre o
 *usuario e a leitura do tom tem que ser zero toques.
 */
@Composable
fun TunerScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.micState) {
        viewModel.startAudioIfPermitted(state.hasPermission)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        TopBar(
            instrument = state.instrument ?: InstrumentCatalog.default,
            tuningLabel = state.tuningLabel.orEmpty(),
            a4Hz = state.a4Hz,
            capoFret = state.capoFret,
            isListening = state.isListening,
            onSelectInstrument = viewModel::selectInstrument,
            onSelectTuning = viewModel::selectTuning,
        )

        Spacer(Modifier.height(8.dp))

        if (state.micState !is MicState.Listening) {
            MicStatusBanner(
                state = state.micState,
                onRequestPermission = onRequestPermission,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        NoteReadout(
            reading = state.reading,
            stringLabel = state.detectedStringIndex?.let { index ->
                state.effectiveTuning?.let { tuning ->
                    stringResource(
                        R.string.string_number,
                        index + 1,
                        MusicTheory.labelOf(tuning.midiNotes[index]),
                    )
                }
            },
        )

        Spacer(Modifier.height(20.dp))

        NoteDial(
            reading = state.reading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(4.dp))

        CentsReadout(reading = state.reading)

        Spacer(Modifier.height(28.dp))

        // A afinação efetiva já tem o capo embutido: com capo na 3ª casa, a 6ª
        // corda de E2 aparece como G2, que é a nota que o músico ouve.
        val tuning = state.effectiveTuning
        if (tuning != null && !state.isChromatic) {
            StringPicker(
                count = tuning.stringCount,
                detectedIndex = state.detectedStringIndex,
                guidedIndex = state.guidedStringIndex,
                labels = tuning.midiNotes.mapIndexed { i, midi ->
                    "Corda ${i + 1}, ${MusicTheory.labelOf(midi)}"
                },
                onSelect = { viewModel.stopGuidedMode() },
            )
            Spacer(Modifier.height(16.dp))
        }

        if (state.isChromatic) {
            Text(
                text = stringResource(R.string.chromatic_mode_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TopBar(
    instrument: Instrument,
    tuningLabel: String,
    a4Hz: Float,
    capoFret: Int,
    isListening: Boolean,
    onSelectInstrument: (String) -> Unit,
    onSelectTuning: (String) -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    var tuningMenuAberto by remember { mutableStateOf(false) }
    val instrumentAtual = InstrumentCatalog.find(instrument.id)

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = instrumentAtual.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "$a4Hz Hz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            RecordingDot(isListening)

            Surface(
                onClick = { menuAberto = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null)
                    Text(stringResource(R.string.change), style = MaterialTheme.typography.labelLarge)
                }
            }

            DropdownMenu(
                expanded = menuAberto,
                onDismissRequest = { menuAberto = false },
            ) {
                InstrumentCatalog.all.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.displayName) },
                        onClick = {
                            onSelectInstrument(item.id)
                            menuAberto = false
                        },
                    )
                }
            }
        }

        // Afinação e capo ficam logo abaixo do instrumento, e não escondidos nos
        // ajustes: são as duas coisas que mais mudam entre Websites, e a linha
        // precisa dizer com clareza o que o medidor está mirando agora.
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = { tuningMenuAberto = true },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = tuningLabel,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            if (capoFret != 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.capo_fret, capoFret),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        DropdownMenu(
            expanded = tuningMenuAberto,
            onDismissRequest = { tuningMenuAberto = false },
        ) {
            instrumentAtual.tunings.forEach { afinacao ->
                DropdownMenuItem(
                    text = { Text(afinacao.label) },
                    onClick = {
                        onSelectTuning(afinacao.id)
                        tuningMenuAberto = false
                    },
                )
            }
        }
    }
}

@Composable
private fun RecordingDot(isListening: Boolean) {
    val cor = if (isListening) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurfaceVariant
    val rotulo = stringResource(if (isListening) R.string.listening else R.string.idle)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics { contentDescription = rotulo },
    ) {
        Surface(
            color = cor,
            shape = CircleShape,
            modifier = Modifier.size(8.dp),
        ) {}
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}