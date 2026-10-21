// FlowRow ainda é experimental no foundation — estabilizou só na Compose 1.8.
@file:OptIn(ExperimentalLayoutApi::class)

package com.afinu.app.feature.chords

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R
import com.afinu.app.core.util.ChordNames
import com.afinu.app.core.util.ChordQuality
import com.afinu.app.core.util.ChordShape
import com.afinu.app.core.util.Fretboard
import com.afinu.app.ui.components.ChipRow

/**
 * Tela de acordes e braço do instrumento.
 *
 * ## Por que o diagrama é desenhado e não vem de imagem
 * Um diagrama pronto só existe para afinação padrão. Com drop D, capo ou ukulele,
 * a mesma figura passa a representar outro acorde — e aí a imagem está errada.
 * Desenhando a partir de [ChordShape.frets] e da afinação efetiva, a figura é
 * sempre coerente com o que o musician vai ouvir.
 *
 * ## A casa inicial desliza
 * Uma forma na 12ª casa ocupa a mesma largura de tela que uma na 1ª. Mostrar
 * sempre da 1ª à 15ª desperdiça a tela e encolhe os pontos até ficarem ilegíveis.
 * Por isso o desenho começa na menor casa da forma.
 */
@Composable
fun ChordsScreen(
    modifier: Modifier = Modifier,
    viewModel: ChordsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.chords),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = buildString {
                append(state.instrumentName)
                if (state.capoFret != 0) append(" · ")
                if (state.capoFret != 0) append(stringResource(R.string.capo_fret, state.capoFret))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.chords_root),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        ChipRow(
            items = (0..11).toList(),
            selected = state.rootSemitones,
            labelOf = { ChordNames.rootLabel(it) },
            descriptionOf = { "Acorde de ${ChordNames.rootLabel(it)}" },
            onSelect = viewModel::setRoot,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.chords_quality),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        ChipRow(
            items = ChordQuality.entries,
            selected = state.quality,
            labelOf = { it.suffix.ifEmpty { "Maior" } },
            onSelect = viewModel::setQuality,
        )

        Spacer(Modifier.height(20.dp))

        state.tuning?.let { tuning ->
            // A nota destacada usa a MESMA referência que a busca do braço:
            // classe de altura a partir do Dó. Sem isso, destacar "Sol" com A4
            // em 432 Hz marcaria a casa errada.
            FretboardView(
                stringCount = tuning.stringCount,
                highlightSemitones = state.highlightSemitones,
                caption = state.highlightSemitones?.let {
                    stringResource(R.string.fretboard_note, ChordNames.rootLabel(it))
                } ?: stringResource(R.string.fretboard),
                tuning = tuning,
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = state.chordName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        // As notas do acorde, na altura em que soam, e tocáveis. Vem logo
        // abaixo do nome porque é a resposta a "o que é isso?": o nome sozinho
        // não diz quais notas formam o acorde, e é justamente isso que o
        // iniciante não sabe.
        ChordNotesStrip(
            tones = state.tones,
            playingMidi = state.playingMidi,
            onPlay = viewModel::tocarNota,
        )

        Spacer(Modifier.height(16.dp))

        if (state.searching) {
            Text(
                text = "…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (!state.hasShapes) {
            Text(
                text = stringResource(R.string.chords_no_shapes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.shapes.forEach { shape ->
                ShapeCard(
                    shape = shape,
                    rootSemitones = state.rootSemitones,
                    playingMidi = state.playingMidi,
                    onPlay = viewModel::tocarNota,
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ShapeCard(
    shape: ChordShape,
    rootSemitones: Int,
    playingMidi: Int?,
    onPlay: (Int) -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChordDiagram(
                frets = shape.frets,
                modifier = Modifier
                    .width(72.dp)
                    .height(96.dp)
                    .semantics { contentDescription = shapeToSpeech(shape) },
            )
            Spacer(Modifier.width(16.dp))
            Column {
                // As notas da pegada também são tocáveis, e não só as do
                // acorde: estas são as alturas **reais** que o músico vai
                // ouvir, com a oitava que a pegada produz. É nelas que ele
                // afina o ouvido.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    shape.notes.forEach { nota ->
                        NoteChip(
                            midi = nota,
                            isRoot = (((nota % 12) + 12) % 12) ==
                                (((rootSemitones % 12) + 12) % 12),
                            isPlaying = nota == playingMidi,
                            onClick = { onPlay(nota) },
                        )
                    }
                }
                if (shape.barres > 0) {
                    Text(
                        text = stringResource(R.string.barre_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun shapeToSpeech(shape: ChordShape): String = buildString {
    append("Forma: ")
    shape.frets.forEachIndexed { i, casa ->
        append("corda ${i + 1}: ")
        append(casa?.toString() ?: "muda")
        if (i < shape.frets.lastIndex) append(", ")
    }
}

/**
 * Diagrama de uma forma de acorde.
 *
 * As cordas são desenhadas da **mais grave para a mais aguda**, que é a ordem
 * em que o diagrama é lido da esquerda para a direita. Desenhar na ordem
 * inversa é um erro comum e deixa o diagrama ilegível para quem toca.
 */
@Composable
fun ChordDiagram(
    frets: List<Int?>,
    modifier: Modifier = Modifier,
) {
    val corda = MaterialTheme.colorScheme.onSurface
    val casa = MaterialTheme.colorScheme.primary
    val dedo = MaterialTheme.colorScheme.primaryContainer
    val texto = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val n = frets.size
        val larguraCasa = size.width / 4f
        val topo = size.height * 0.18f
        val alturaUtil = size.height * 0.72f
        val passoCorda = if (n > 1) size.width / (n - 1) else size.width

        // Base e travessa
        drawLine(
            color = texto,
            start = Offset(0f, topo - 6f),
            end = Offset(size.width, topo - 6f),
            strokeWidth = 2.5f,
        )

        // Cordas
        repeat(n) { i ->
            val x = i * passoCorda
            drawLine(
                color = corda,
                start = Offset(x, topo),
                end = Offset(x, topo + alturaUtil),
                strokeWidth = 1.5f,
            )
        }

        // Casas visíveis
        val primeira = frets.filterNotNull().minOrNull() ?: 0
        repeat(4) { linha ->
            val y = topo + linha * alturaUtil / 3f
            drawLine(
                color = if (linha == 0) texto else casa,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.2f,
            )
        }

        // Dedos
        frets.forEachIndexed { i, casaNumero ->
            val casaEfetiva = (casaNumero ?: 0) - primeira
            if (casaNumero == null) {
                val x = i * passoCorda
                val y = topo - 12f
                drawLine(
                    color = texto,
                    start = Offset(x - 5f, y - 4f),
                    end = Offset(x + 5f, y + 4f),
                    strokeWidth = 2f,
                )
                drawLine(
                    color = texto,
                    start = Offset(x + 5f, y - 4f),
                    end = Offset(x - 5f, y + 4f),
                    strokeWidth = 2f,
                )
            } else if (casaEfetiva in 0..3) {
                drawCircle(
                    color = dedo,
                    radius = larguraCasa * 0.32f,
                    center = Offset(
                        i * passoCorda,
                        topo + casaEfetiva * alturaUtil / 3f,
                    ),
                )
            }
        }
    }
}

/**
 * Braço do instrumento com uma nota destacada.
 *
 * Diferente do diagrama, aqui o braço é **todo** — o jogador precisa varrer as
 * 15 casas para achar onde a nota aparece. Por isso cada casa é uma linha
 * vertical fina e a nota destacada usa um ponto cheio.
 */
@Composable
fun FretboardView(
    stringCount: Int,
    highlightSemitones: Int?,
    caption: String,
    tuning: com.afinu.app.core.model.TuningPreset,
    modifier: Modifier = Modifier,
) {
    // As posições vêm do mesmo Fretboard que o afinador usa, então o que está
    // marcado aqui é exatamente o que a nota soa na afinação atual.
    val destacadas = remember(highlightSemitones, tuning, stringCount) {
        highlightSemitones?.let { Fretboard.positionsForNoteName(tuning, ChordNames.rootLabel(it)) }
            ?: emptyList()
    }
    val casasMarcadas = remember(destacadas) { destacadas.map { it.stringIndex to it.fret }.toSet() }

    Column(modifier = modifier) {
        Text(
            text = caption,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))

        val linha = MaterialTheme.colorScheme.surfaceVariant
        val marca = MaterialTheme.colorScheme.primary

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .semantics {
                    contentDescription = buildString {
                        append(caption)
                        append(", $stringCount cordas")
                        if (highlightSemitones != null) {
                            append(", ")
                            append(destacadas.size)
                            append(" posições de ")
                            append(ChordNames.rootLabel(highlightSemitones))
                        }
                    }
                },
        ) {
            val passoCorda = if (stringCount > 1) size.width / (stringCount - 1) else size.width
            val passoCasa = size.height / 15f

            repeat(stringCount) { i ->
                val x = i * passoCorda
                drawLine(linha, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
            }
            repeat(16) { casa ->
                val y = casa * passoCasa
                drawLine(linha, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            casasMarcadas.forEach { (corda, casa) ->
                drawCircle(
                    color = marca,
                    radius = 7f,
                    center = Offset(
                        corda * passoCorda,
                        casa * passoCasa + passoCasa / 2f,
                    ),
                )
            }
        }
    }
}