package com.afinu.app.feature.chords

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.afinu.app.R
import com.afinu.app.core.util.ChordTone
import com.afinu.app.core.util.MusicTheory

/**
 * As notas que compõem o acorde, prontas para ouvir.
 *
 * ## Por que a fundamental é diferente
 * A tônica é o que dá nome ao acorde: "Dó" é o Dó maior, e não outra coisa.
 * Quem está começando não distingue a tônica das outras notas sem que isso
 * apareça — e sem isso não sabe o que está ouvindo nem o que vai procurar no
 * braço. Então a fundamental vem preenchida e em negrito, e as demais ficam
 * neutras.
 *
 * ## Por que a nota que está soando muda de cor
 * O som é o que confirma o toque, mas o olho precisa acompanhar: sem isso o
 * usuário toca duas vezes seguidas e não sabe qual nota ouviu. A nota que
 * está soando usa a cor secundária — que, no app, é a cor de "afinou".
 *
 * ## Por que o fluxo e não uma fileira
 * `FlowRow` quebra a linha quando não cabe. Numa janela estreita de celular
 * as notas ocupam duas linhas; numa janela larga de Chromebook, uma só. O
 * mesmo componente serve aos dois sem medida fixa.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChordNotesStrip(
    tones: List<ChordTone>,
    playingMidi: Int?,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tones.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.chords_notes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tones.forEach { tone ->
                NoteChip(
                    midi = tone.midi,
                    isRoot = tone.isRoot,
                    isPlaying = tone.midi == playingMidi,
                    onClick = { onPlay(tone.midi) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.chords_notes_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Uma nota do acorde, tocável.
 *
 * O rótulo é o nome semiatônico com a oitava ("C3"), que é o que o musician
 * lê no braço. A oitava importa: o mesmo "C" em duas oitavas são duas notas
 * diferentes, e a que soa é a que está na pegada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteChip(
    midi: Int,
    isRoot: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val rotulo = MusicTheory.labelOf(midi)
    val descricao = buildString {
        append(rotulo)
        if (isRoot) {
            append(", ")
            append(stringResource(R.string.chords_root_note))
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = when {
            isPlaying -> MaterialTheme.colorScheme.secondaryContainer
            isRoot -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when {
            isPlaying -> MaterialTheme.colorScheme.onSecondaryContainer
            isRoot -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (isPlaying) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        } else {
            null
        },
    ) {
        Text(
            text = rotulo,
            style = if (isRoot) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (isRoot || isPlaying) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                // A descrição carrega o papel da nota ("C3, fundamental"). O
                // leitor de tela prefere a descrição ao texto, então o nome não
                // é anunciado duas vezes.
                .semantics { contentDescription = descricao },
        )
    }
}
