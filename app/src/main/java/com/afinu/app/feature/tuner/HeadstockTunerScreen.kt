package com.afinu.app.feature.tuner

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.ui.components.StudioActionRow
import com.afinu.app.ui.components.StudioCard
import com.afinu.app.ui.components.StudioCheckMark
import com.afinu.app.ui.components.StudioDotSwitch
import com.afinu.app.ui.components.StudioStatusPill
import com.afinu.app.ui.theme.DataMonoMedium
import com.afinu.app.ui.theme.LabelCaps
import java.util.Locale

/**
 * O modo violão: uma cravelha por corda, com o braço ao centro.
 *
 * ## Por que este modo é o principal
 * O afinador de violão não é um instrumento de precisão: é um jogo de
 * afinar seis cordas uma a uma, lembrando onde cada uma parou. O painel de
 * cravelhas mostra as seis ao mesmo tempo, e é isso que o jogador precisa
 * ver enquanto aperta a cravelha.
 *
 * O modo cromático, ao lado, serve para afinar qualquer outra coisa.
 */
@Composable
fun HeadstockTunerScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.micState, state.hasPermission) {
        viewModel.startAudioIfPermitted(state.hasPermission)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        InstrumentRow(
            nomeInstrumento = state.instrument?.displayName ?: "",
            rotuloAfinacao = state.tuningLabel ?: "",
            onClick = { },
        )

        Spacer(Modifier.height(12.dp))

        // O estado da corda que está soando agora.
        PegAtivoBanner(
            state = state,
            onTocarReferencia = { midi -> viewModel.tocarReferencia(midi) },
        )

        Spacer(Modifier.height(16.dp))

        // A craveelha e o braço.
        HeadstockStage(
            midiNotes = state.effectiveTuning?.midiNotes ?: emptyList(),
            status = state.stringStatus,
            cordaAtiva = state.detectedStringIndex,
            a4Hz = state.a4Hz,
        )

        Spacer(Modifier.height(20.dp))

        // Ações.
        StudioActionRow(
            icon = Icons.Default.GraphicEq,
            title = stringResource(R.string.headstock_ear_training),
            subtitle = stringResource(R.string.headstock_ear_training_hint),
            tint = MaterialTheme.colorScheme.secondary,
            trailing = {
                if (state.tocandoSequencia) {
                    StudioStatusPill(text = stringResource(R.string.headstock_playing))
                }
            },
            onClick = {
                if (state.tocandoSequencia) viewModel.pararSequencia() else viewModel.tocarSequencia()
            },
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StudioActionRow(
                icon = Icons.Default.GraphicEq,
                title = stringResource(R.string.headstock_auto),
                subtitle = stringResource(R.string.headstock_auto_hint),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                trailing = {
                    StudioDotSwitch(
                        ligado = state.autoDetect,
                        onToggle = { viewModel.setAutoDetect(!state.autoDetect) },
                        descricao = stringResource(R.string.headstock_auto),
                    )
                },
            )
            StudioActionRow(
                icon = Icons.Default.GraphicEq,
                title = stringResource(R.string.headstock_loop),
                subtitle = stringResource(
                    if (state.loopContinuo) R.string.headstock_loop_on else R.string.headstock_loop_off,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                trailing = {
                    StudioDotSwitch(
                        ligado = state.loopContinuo,
                        onToggle = { viewModel.setLoopContinuo(!state.loopContinuo) },
                        descricao = stringResource(R.string.headstock_loop),
                    )
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        if (state.micState !is MicState.Listening) {
            MicStatusBanner(
                state = state.micState,
                onRequestPermission = onRequestPermission,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

/**
 * A linha do instrumento: o nome, a afinação e o A4.
 *
 * A pílula do instrumento é o que o design chama de "preset selector": ela
 * mostra o que está sendo afinado e, logo abaixo, com qual afinação.
 */
@Composable
private fun InstrumentRow(
    nomeInstrumento: String,
    rotuloAfinacao: String,
    onClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StudioCard(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = nomeInstrumento,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = rotuloAfinacao.uppercase(),
                        style = LabelCaps.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * A faixa da corda que está soando.
 *
 * É o resumo numérico do que está acontecendo agora: qual nota, para onde
 * ela deveria ir, e o quanto falta. E o botão verde que toca a nota alvo —
 * quem afina de ouvido precisa da referência na mão.
 */
@Composable
private fun PegAtivoBanner(
    state: TunerUiState,
    onTocarReferencia: (Int) -> Unit,
) {
    val status = state.detectedStringIndex?.let { state.stringStatus[it] }
    val midi = status?.midi ?: state.effectiveTuning?.midiNotes?.getOrNull(
        state.detectedStringIndex ?: 0,
    )
    val (letra, oitava) = if (midi != null) {
        notaEmPartes(midi)
    } else {
        "—" to ""
    }

    StudioCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letra,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                if (oitava.isNotEmpty()) {
                    Text(
                        text = oitava,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.detectedStringIndex?.let { indice ->
                            // A afinação vai do grave ao agudo, e a numeração
                            // das cordas é ao contrário: a 6ª é a índice 0.
                            stringResource(R.string.headstock_active_string, 6 - indice)
                        } ?: stringResource(R.string.headstock_no_string),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(8.dp))
                    StudioStatusPill(text = stringResource(R.string.listening))
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.headstock_target_deviation,
                        formatarHertz(status?.frequencyHz ?: 0f),
                        formatarCents(status?.cents),
                    ),
                    style = DataMonoMedium.copy(fontSize = DataMonoMedium.fontSize * 0.78f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(enabled = midi != null) { midi?.let(onTocarReferencia) }
                    .semantics {
                        contentDescription = "Tocar a nota de referência"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * O palco: três cravelhas à esquerda, o braço ao centro, três à direita.
 *
 * ## Por que a cravelha é uma lista, e não um botão
 * Porque ela mostra o **estado** de cada corda: o verde é "no ponto", a
 * seta para cima é "apertar", a seta para baixo é "afrouxar". O jogador não
 * precisa apertar cravelha nenhuma para ler isso — a posição das cordas no
 * violão é um relógio.
 */
@Composable
private fun HeadstockStage(
    midiNotes: List<Int>,
    status: Map<Int, StringStatus>,
    cordaAtiva: Int?,
    a4Hz: Float,
) {
    if (midiNotes.isEmpty()) return

    val metade = midiNotes.size / 2
    val esquerda = midiNotes.indices.take(metade).toList()
    val direita = midiNotes.indices.drop(metade).reversed().toList()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
        contentAlignment = Alignment.Center,
    ) {
        // O braço, atrás das cravelhas.
        HeadstockGraphic(
            midiNotes = midiNotes,
            status = status,
            cordaAtiva = cordaAtiva,
            modifier = Modifier
                .width(140.dp)
                .height(370.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(370.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.width(132.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                esquerda.forEach { indice ->
                    PegCard(
                        midi = midiNotes[indice],
                        status = status[indice],
                        ativo = cordaAtiva == indice,
                        a4Hz = a4Hz,
                    )
                }
            }
            Column(
                modifier = Modifier.width(132.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                direita.forEach { indice ->
                    PegCard(
                        midi = midiNotes[indice],
                        status = status[indice],
                        ativo = cordaAtiva == indice,
                        a4Hz = a4Hz,
                    )
                }
            }
        }
    }
}

/**
 * O braço do violão, desenhado.
 *
 * ## Por que desenhar e não usar uma imagem
 * Um desenho pronto é sempre o mesmo violão. Com drop D, com capo, com 7 ou 8
 * cordas, o braço é outro. Desenhando a partir da afinação efetiva, o que
 * aparece é sempre o instrumento que está na mão.
 */
@Composable
private fun HeadstockGraphic(
    midiNotes: List<Int>,
    status: Map<Int, StringStatus>,
    cordaAtiva: Int?,
    modifier: Modifier = Modifier,
) {
    val madeira = Brush.verticalGradient(
        listOf(Color(0xFF1C1613), Color(0xFF120F0E), Color(0xFF0A0A0C)),
    )
    val cordaAtivaCor = MaterialTheme.colorScheme.primary
    val cordaCor = Color(0xFF9AA3B2)

    Canvas(modifier = modifier) {
        val largura = size.width
        val altura = size.height
        val topoPino = altura * 0.10f
        val base = altura * 0.94f

        // O corpo do braço: fundo arredondado em cima, reto embaixo.
        drawRoundRect(
            brush = madeira,
            topLeft = Offset(0f, 0f),
            size = Size(largura, altura),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(largura * 0.28f, largura * 0.28f),
        )

        // A linha central do veio da madeira.
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(largura / 2f, 0f),
            end = Offset(largura / 2f, altura),
            strokeWidth = 1f,
        )

        // As cordas: da cravelha até a ponteira.
        val n = midiNotes.size
        val passo = largura / (n + 1f)
        midiNotes.forEachIndexed { indice, _ ->
            val x = passo * (indice + 1)
            val afinada = status[indice]?.inTune == true
            val cor = when {
                indice == cordaAtiva -> cordaAtivaCor
                afinada -> cordaAtivaCor.copy(alpha = 0.35f)
                else -> cordaCor.copy(alpha = 0.55f)
            }
            val espessura = (3.4f - indice * 0.42f).coerceAtLeast(1.1f)
            drawLine(
                color = cor,
                start = Offset(x, topoPino),
                end = Offset(x, base),
                strokeWidth = espessura,
            )
        }

        // Os pinos, três de cada lado.
        val pinosPorLado = (n + 1) / 2
        for (i in 0 until pinosPorLado) {
            val y = topoPino + (base - topoPino) * (i + 0.5f) / pinosPorLado
            val xEsq = passo * (i + 1)
            val xDir = largura - passo * (i + 1)
            drawCircle(color = Color(0xFF3A3F4B), radius = 5.5f, center = Offset(xEsq, y))
            drawCircle(color = Color(0xFF3A3F4B), radius = 5.5f, center = Offset(xDir, y))
        }

        // A ponteira, no pé do braço.
        drawRoundRect(
            color = Color(0xFFCDBFA8),
            topLeft = Offset(passo * 0.5f, base),
            size = Size(largura - passo, altura * 0.045f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
        )
    }
}

/**
 * A cravelha de uma corda.
 *
 * O estado tem três leituras possíveis, e o desenho de cada uma é diferente
 * porque cada uma pede uma **ação** diferente:
 *
 * - verde com visto: está no ponto, não mexa
 * - seta para cima: apertar (a nota está baixa)
 * - seta para baixo: afrouxar (a nota está alta)
 */
@Composable
private fun PegCard(
    midi: Int,
    status: StringStatus?,
    ativo: Boolean,
    a4Hz: Float,
) {
    val (letra, oitava) = notaEmPartes(midi)
    val cents = status?.cents
    val afinada = status?.inTune == true

    val corDoEstado = when {
        ativo -> MaterialTheme.colorScheme.primary
        afinada -> MaterialTheme.colorScheme.primary
        cents != null -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    StudioCard(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .border(
                width = if (ativo) 2.dp else 1.dp,
                color = if (ativo) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White.copy(alpha = 0.06f)
                },
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ativo) {
                PulsingIcon()
            } else if (afinada) {
                StudioCheckMark(size = 34.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (cents != null && cents > 0f) {
                            Icons.Default.ArrowDownward
                        } else {
                            Icons.Default.ArrowUpward
                        },
                        contentDescription = null,
                        tint = corDoEstado,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = letra,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (ativo) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                    )
                    Text(
                        text = oitava,
                        style = DataMonoMedium.copy(fontSize = DataMonoMedium.fontSize * 0.7f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                    if (cents != null) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = formatarCents(cents),
                            style = LabelCaps.copy(fontSize = 9.sp),
                            color = corDoEstado,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = formatarHertzAlvo(midi, a4Hz),
                    style = DataMonoMedium.copy(fontSize = DataMonoMedium.fontSize * 0.72f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * O ícone que respira na corda que está soando.
 *
 * A pulsação é o equivalente visual do "estou te ouvindo": sem ela, a
 * cravelha ativa seria só mais uma highlighted na lista.
 */
@Composable
private fun PulsingIcon() {
    val transicao = rememberInfiniteTransition(label = "pulso")
    val alpha by transicao.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Separa a nota em letra e oitava: "E2" vira ("E", "2").
 *
 * A oitava vai menor e ao lado, como no desenho. É o mesmo truque do
 * design: a letra é o que se lê de longe, e a oitava é o detalhe.
 */
internal fun notaEmPartes(midi: Int): Pair<String, String> {
    val texto = MusicTheory.labelOf(midi)
    if (texto.isEmpty()) return "" to ""
    return texto.dropLast(1) to texto.last().toString()
}

/** "82.4 Hz" — uma casa decimal, como no desenho. */
internal fun formatarHertz(hz: Float): String =
    if (hz <= 0f) "-- Hz" else String.format(Locale.getDefault(), "%.1f Hz", hz)

/** A frequência **alvo** de uma nota, na afinação e no A4 do usuário. */
internal fun formatarHertzAlvo(midi: Int, a4Hz: Float = 440f): String =
    String.format(Locale.getDefault(), "%.1f Hz", MusicTheory.frequencyOf(midi, a4Hz))

/** "0¢", "+0.2¢", "-4¢". */
internal fun formatarCents(cents: Float?): String = when {
    cents == null -> "—"
    cents == 0f -> "0¢"
    kotlin.math.abs(cents) < 1f -> String.format(Locale.getDefault(), "%+.1f¢", cents)
    else -> String.format(Locale.getDefault(), "%+.0f¢", cents)
}
