package com.afinu.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R
import com.afinu.app.core.model.TuningPreset
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.ui.components.StudioCard
import com.afinu.app.ui.components.StudioSegmentedControl
import com.afinu.app.ui.components.StudioStatusPill
import com.afinu.app.ui.theme.DataMonoMedium
import com.afinu.app.ui.theme.LabelCaps
import java.util.Locale

/**
 * A biblioteca de afinações.
 *
 * ## Por que a biblioteca é uma lista de cartões, e não uma lista de chips
 * Porque uma afinação **é** uma frase: "DADGAD" com a nota de cada corda é
 * uma informação, não um rótulo. O chip cabia a palavra e perdia o resto.
 *
 * ## O que o cartão mostra
 * O nome, o perfil, a nota de cada corda com sua frequência, e a tensão. A
 * tensão é a informação que ninguém tem: duas afinações com as mesmas letras
 * podem soar bem diferentes, e é a mais grave que deixa o instrumento pesado.
 */
@Composable
fun TuningLibraryScreen(
    /** Abre os Ajustes, que moram dentro desta aba e não são uma delas. */
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var busca by remember { mutableStateOf("") }
    var secao by remember { mutableIntStateOf(0) }
    var categoria by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // A biblioteca, os acordes e as notas de referência dividem a aba.
        StudioSegmentedControl(
            opcoes = listOf(
                stringResource(R.string.library_tunings),
                stringResource(R.string.library_chords),
                stringResource(R.string.library_notes),
            ),
            selecionada = secao,
            onSelect = { secao = it },
        )

        Spacer(Modifier.height(16.dp))

        // ## Por que cada seção rola por si só
        // A tela de acordes e a de notas já têm a própria rolagem. Aninhar
        // duas rolagens verticais numa Column dá ao Compose uma altura
        // infinita, e ele derruba o app com "vertically scrollable component
        // was measured with an infinity maximum height constraints".
        //
        // Então o contêiner de fora não rola: cada seção traz a sua.
        when (secao) {
            1 -> com.afinu.app.feature.chords.ChordsScreen(modifier = Modifier.fillMaxWidth())
            2 -> com.afinu.app.feature.notes.NotesCardScreen(modifier = Modifier.fillMaxWidth())
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
        // A busca e o botão de Ajustes, lado a lado.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { campo ->
                        if (busca.isEmpty()) {
                            Text(
                                text = stringResource(R.string.library_search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        campo()
                    },
                )
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // O instrumento e quantas afinações ele tem.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.instrument.displayName.uppercase(),
                style = LabelCaps,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.library_count, state.instrument.tunings.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        // As categorias, derivadas das afinações do instrumento.
        val categorias = remember(state.instrument.id) {
            listOf("") + state.instrument.tunings.mapNotNull { perfilDe(it)?.categoria }.distinct()
        }
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categorias.size) { indice ->
                val valor = categorias[indice]
                val ativo = valor == categoria
                Text(
                    text = if (valor.isEmpty()) {
                        stringResource(R.string.library_all)
                    } else {
                        valor.uppercase()
                    },
                    style = LabelCaps,
                    color = if (ativo) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (ativo) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        )
                        .clickable { categoria = valor }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Os cartões.
        val visiveis = state.instrument.tunings.filter { afinacao ->
            val passaBusca = busca.isBlank() || afinacao.label.contains(busca, ignoreCase = true)
            val passaCategoria = categoria.isEmpty() || perfilDe(afinacao)?.categoria == categoria
            passaBusca && passaCategoria
        }
        if (visiveis.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        visiveis.forEach { afinacao ->
            TuningCard(
                tuning = afinacao,
                emUso = afinacao.id == state.tuning.id,
                a4Hz = state.a4Hz,
                onApply = { viewModel.selectTuning(afinacao.id) },
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(4.dp))

        // Criar afinação: entra nos favoritos, que é onde uma afinação nova
        // vive antes de virar um preset do catálogo.
        StudioCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.library_create).uppercase(),
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/**
 * O cartão de uma afinação.
 */
@Composable
private fun TuningCard(
    tuning: TuningPreset,
    emUso: Boolean,
    a4Hz: Float,
    onApply: () -> Unit,
) {
    val perfil = remember(tuning.id) { perfilDe(tuning) }

    StudioCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (emUso) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    Color.White.copy(alpha = 0.06f)
                },
                shape = MaterialTheme.shapes.large,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tuning.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (perfil != null) {
                    Spacer(Modifier.width(8.dp))
                    StudioStatusPill(
                        text = perfil.etiqueta,
                        color = MaterialTheme.colorScheme.secondary,
                        background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (emUso) {
                    StudioStatusPill(text = stringResource(R.string.library_in_use))
                }
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = stringResource(R.string.library_preview),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { },
                )
            }

            Spacer(Modifier.height(12.dp))

            // A matriz de notas: a informação que faz o cartão ser um cartão.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tuning.midiNotes.forEachIndexed { indice, midi ->
                    CelulaDeNota(
                        numero = tuning.midiNotes.size - indice,
                        midi = midi,
                        a4Hz = a4Hz,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.library_tension, rotuloDeTensao(tuning)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (!emUso) {
                    Text(
                        text = stringResource(R.string.library_apply).uppercase(),
                        style = LabelCaps,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onApply)
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}

/**
 * A célula de uma corda: número da corda, nota e frequência.
 */
@Composable
private fun CelulaDeNota(
    numero: Int,
    midi: Int,
    a4Hz: Float,
    modifier: Modifier = Modifier,
) {
    val rotulo = MusicTheory.labelOf(midi)
    val letra = rotulo.dropLast(1)
    val oitava = rotulo.last().toString()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(vertical = 7.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${numero}ª",
            style = LabelCaps.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = letra,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = oitava,
                style = DataMonoMedium.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            text = String.format(
                Locale.getDefault(),
                "%.0fHz",
                MusicTheory.frequencyOf(midi, a4Hz),
            ),
            style = DataMonoMedium.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
        )
    }
}

/**
 * O perfil de uma afinação: a etiqueta e a categoria.
 *
 * ## De onde vem
 * O catálogo tem nome e notas, e não tem descrição nem etiqueta. Derivar as
 * duas do próprio nome e das notas é melhor do que inventar um campo novo no
 * catálogo: a etiqueta "PESADA" para um drop D é dedutível, e uma descrição
 * escrita à mão para 34 instrumentos seria texto que ninguém lê.
 */
private data class Perfil(
    val etiqueta: String,
    val categoria: String,
)

private fun perfilDe(tuning: TuningPreset): Perfil? {
    val rotulo = tuning.label.lowercase(Locale.getDefault())
    return when {
        rotulo.contains("drop") -> Perfil("PESADA", "Rock & Metal")
        rotulo.contains("dadgad") || rotulo.contains("daga") ->
            Perfil("CELTIC", "Folk")
        rotulo.contains("open") -> Perfil("BLUES", "Blues")
        rotulo.contains("meio tom") || rotulo.contains("semitom") ->
            Perfil("HENDRIX", "Rock & Metal")
        rotulo.contains("slap") -> Perfil("SLAP", "Rock & Metal")
        rotulo.contains("12") -> Perfil("12 CORDAS", "Rock & Metal")
        else -> null
    }
}

/**
 * A tensão de uma afinação, pelo que a nota mais grave diz.
 *
 * Uma corda grave é pesada: precisa de mais tensão e de mais braço. É por
 * isso que um drop D soa "mais apertado" que um padrão, e o motivo de um
 * instrumento trocar de afinação e não aguentar.
 */
private fun rotuloDeTensao(tuning: TuningPreset): String = when {
    tuning.lowestMidi <= 36 -> "Pesada"
    tuning.lowestMidi <= 40 -> "Média-Baixa"
    tuning.lowestMidi <= 45 -> "Média"
    else -> "Leve"
}
