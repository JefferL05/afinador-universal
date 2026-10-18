package com.afinu.app.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R
import com.afinu.app.core.model.NOTE_NAMES
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/** Uma linha do cartão: nome da nota, valor em cents e frequência. */
data class NoteRow(
    val midi: Int,
    val label: String,
    val frequencyHz: Float,
    val centsFromA4: Int,
)

data class NotesCardUiState(
    val a4Hz: Float = 440f,
    val rows: List<NoteRow> = emptyList(),
)

/**
 * Cartão de notas.
 *
 * ## Por que a frequência importa mais que a partitura
 * Um músico que afina "de ouvido", tocando junto com outro instrumento, precisa
 * do número em hertz — é isso que ele compara com o afinador do outro. A
 * partitura Help/help não serve para isso. Por isso a frequência é o número
 * grande, e o nome da nota é o rótulo pequeno.
 *
 * ## Por que só as 12 notas
 * As oitavas são consequência da frequência. Se o cartão mostrasse 88 teclas,
 * a maior parte das células seria "esta é a mesma nota que a de cima, uma
 * oitava acima", que é informação que não ajuda a decidir. O cartão dá a régua
 * e o resto é repetição.
 */
@HiltViewModel
class NotesCardViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<NotesCardUiState> = settingsRepository.settings
        .map { prefs -> prefs.a4Hz to prefs.pitchOffsetCents }
        .map { (a4Hz, offsetCents) -> buildState(a4Hz, offsetCents) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesCardUiState())

    private fun buildState(a4Hz: Float, offsetCents: Float): NotesCardUiState {
        // O deslocamento global entra aqui também: se o usuário calibrou o app
        // para afinar tudo 3 cents mais agudo, o cartão precisa mostrar a
        // frequência que o app considera correta.
        val a4Efetivo = MusicTheory.frequencyOf(
            MusicTheory.MIDI_A4,
            a4Hz,
        ) * kotlin.math.exp(offsetCents / 1200f * kotlin.math.ln(2.0).toFloat())

        val rows = (60..71).map { midi ->
            val hz = MusicTheory.frequencyOf(midi, a4Efetivo)
            NoteRow(
                midi = midi,
                label = MusicTheory.labelOf(midi),
                frequencyHz = hz,
                centsFromA4 = (midi - MusicTheory.MIDI_A4) * 100,
            )
        }
        return NotesCardUiState(a4Hz = a4Efetivo, rows = rows)
    }
}

@Composable
fun NotesCardScreen(
    modifier: Modifier = Modifier,
    viewModel: NotesCardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.notes_card),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.a4_reference_value, state.a4Hz.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(state.rows, key = { it.midi }) { row -> NoteCard(row) }
        }
    }
}

@Composable
private fun NoteCard(row: NoteRow) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.semantics {
            contentDescription = "${row.label}, ${"%.2f".format(row.frequencyHz)} hertz"
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (row.centsFromA4 >= 0) "+${row.centsFromA4} cents"
                    else "${row.centsFromA4} cents",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                Text(
                    text = "%.2f".format(row.frequencyHz),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Mantido para consultas por semitom na tela de acordes (fase 5). */
internal fun nameOf(semitoneFromC: Int): String = NOTE_NAMES[semitoneFromC % 12]