package com.afinu.app.feature.tuner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R
import com.afinu.app.ui.components.StudioActionRow
import com.afinu.app.ui.components.StudioCard
import com.afinu.app.ui.components.StudioReadoutCard
import com.afinu.app.ui.components.StudioSegmentedControl
import com.afinu.app.ui.components.StudioStatusPill
import com.afinu.app.ui.theme.DataMonoMedium
import com.afinu.app.ui.theme.DisplayNote
import com.afinu.app.ui.theme.LabelCaps
import kotlin.math.cos
import kotlin.math.sin

/**
 * O afinador cromático: qualquer nota, com o medidor de arco.
 *
 * ## Para quem é
 * Para afinar o que não tem seis cordas: um violino (que se afina por
 * quintas, não por oitavas), um ukulele, um instrumento de sopro, ou o
 * resulta de um estudio caseiro. E para quem quer saber que nota está
 * soando, sem se importar com a afinação do instrumento.
 *
 * ## O que o modo violão não faz
 * Ele não afina "qualquer nota": ele afina as seis cordas de um instrumento
 * com afinação fixa. O cromático é o outro lado da moeda.
 */
@Composable
fun ChromaticTunerScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TunerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // `state` é uma propriedade delegada, e o compilador não consegue estreitar
    // o tipo dela dentro de um `if`. Uma cópia local resolve.
    val leitura = state.reading

    LaunchedEffect(state.micState, state.hasPermission) {
        viewModel.startAudioIfPermitted(state.hasPermission)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // Modo de leitura.
        StudioSegmentedControl(
            opcoes = listOf(
                stringResource(R.string.chromatic_auto),
                stringResource(R.string.chromatic_manual),
            ),
            selecionada = 0,
            onSelect = { },
        )

        Spacer(Modifier.height(12.dp))

        // O nível do microfone: mostra que o app está ouvindo.
        NivelMicrofonePill(
            ouvindo = state.micState is MicState.Listening,
            hz = state.smoothedFrequencyHz,
        )

        Spacer(Modifier.height(16.dp))

        // O medidor.
        StudioCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MedidorDeArco(
                    cents = state.reading?.cents,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.15f),
                )
                Spacer(Modifier.height(4.dp))
                // A nota, enorme: é o que se lê de longe.
                if (leitura != null) {
                    val (letra, oitava) = notaEmPartes(leitura.midi)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = letra,
                            style = DisplayNote,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = oitava,
                            style = DisplayNote.copy(fontSize = DisplayNote.fontSize * 0.42f),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    StudioStatusPill(
                        text = if (leitura.inTune) {
                            stringResource(R.string.chromatic_in_tune)
                        } else {
                            stringResource(R.string.chromatic_listening)
                        },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.chromatic_waiting),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // As duas leituras numéricas.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StudioReadoutCard(
                label = stringResource(R.string.chromatic_cents),
                value = leitura?.let { formatarCents(it.cents).removeSuffix("¢") } ?: "—",
                suffix = "ct",
                valueColor = if (leitura?.inTune == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            StudioReadoutCard(
                label = stringResource(R.string.chromatic_real_frequency),
                value = leitura?.let {
                    String.format(java.util.Locale.getDefault(), "%.2f", it.frequencyHz)
                } ?: "—",
                suffix = "Hz",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        // As cordas do instrumento, para tocar e ouvir a referência.
        val cordas = state.effectiveTuning?.midiNotes.orEmpty()
        if (cordas.isNotEmpty()) {
            Text(
                text = stringResource(R.string.chromatic_strings).uppercase(),
                style = LabelCaps.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cordas.forEachIndexed { indice, midi ->
                    SeletorDeCorda(
                        midi = midi,
                        numero = cordas.size - indice,
                        ativo = state.detectedStringIndex == indice,
                        onClick = { viewModel.tocarReferencia(midi) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Ferramentas de áudio.
        StudioActionRow(
            icon = Icons.Default.Mic,
            title = stringResource(R.string.chromatic_noise_filter),
            subtitle = stringResource(R.string.chromatic_noise_filter_on),
            tint = MaterialTheme.colorScheme.secondary,
        )

        Spacer(Modifier.height(10.dp))

        StudioActionRow(
            icon = Icons.Default.VolumeUp,
            title = stringResource(R.string.chromatic_emit_tone),
            subtitle = stringResource(R.string.chromatic_emit_tone_hint),
            tint = MaterialTheme.colorScheme.secondary,
            onClick = {
                leitura?.let { viewModel.tocarReferencia(it.midi) }
            },
        )

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
 * O medidor de arco: 180 graus, com a agulha no zero.
 *
 * ## Por que um arco e não uma barra
 * Uma barra linear de desvio obriga o olho a ir até uma régua e ler um
 * número. Um arco coloca o zero no ponto onde o olho já está, e o desvio
 * vira **distância**, não leitura. O músico vê "está para lá" antes de ver
 * "está 12 cents acima".
 *
 * ## Por que os traços são finos
 * A 1 cent de resolução num arco de 180 graus, um traço de 1 px é o mínimo
 * que não some. Traços mais grossos viram uma serrilha e escondem a posição.
 */
@Composable
private fun MedidorDeArco(
    cents: Float?,
    modifier: Modifier = Modifier,
) {
    val corAtiva = MaterialTheme.colorScheme.primary
    val corInativa = MaterialTheme.colorScheme.outlineVariant
    val corDoTexto = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val centro = Offset(size.width / 2f, size.height * 0.92f)
        val raio = size.minDimension * 0.82f
        val espessura = 3.dp.toPx()

        // O arco de fundo, de -50 a +50.
        drawArc(
            color = corInativa,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centro.x - raio, centro.y - raio),
            size = Size(raio * 2, raio * 2),
            style = Stroke(width = espessura, cap = StrokeCap.Round),
        )

        // A zona no ponto, um pouco mais espessa e mais clara.
        val zonaGraus = 3f / 50f * 180f
        drawArc(
            color = corAtiva.copy(alpha = 0.35f),
            startAngle = 90f - zonaGraus / 2f,
            sweepAngle = zonaGraus,
            useCenter = false,
            topLeft = Offset(centro.x - raio, centro.y - raio),
            size = Size(raio * 2, raio * 2),
            style = Stroke(width = espessura * 2.2f, cap = StrokeCap.Round),
        )

        // Os traços, de 10 em 10 cents.
        for (c in -50..50 step 10) {
            val angulo = Math.toRadians(90.0 - c / 50.0 * 90.0)
            val maior = c % 25 == 0
            val rExterno = raio * (if (maior) 0.86f else 0.91f)
            val rInterno = raio * 0.96f
            drawLine(
                color = if (maior) corDoTexto else corInativa,
                start = Offset(
                    centro.x + (rExterno * cos(angulo)).toFloat(),
                    centro.y - (rExterno * sin(angulo)).toFloat(),
                ),
                end = Offset(
                    centro.x + (rInterno * cos(angulo)).toFloat(),
                    centro.y - (rInterno * sin(angulo)).toFloat(),
                ),
                strokeWidth = if (maior) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // A agulha.
        if (cents != null) {
            val limitado = cents.coerceIn(-50f, 50f)
            val angulo = Math.toRadians(90.0 - limitado / 50.0 * 90.0)
            val ponta = Offset(
                centro.x + (raio * 0.82f * cos(angulo)).toFloat(),
                centro.y - (raio * 0.82f * sin(angulo)).toFloat(),
            )
            drawLine(
                color = corAtiva,
                start = centro,
                end = ponta,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = corAtiva, radius = 7.dp.toPx(), center = centro)
        } else {
            drawCircle(color = corInativa, radius = 7.dp.toPx(), center = centro)
        }
    }
}

/**
 * A pastilha do nível do microfone.
 *
 * Existe para uma coisa só: provar que o app está ouvindo. Sem ela, o
 * usuário não sabe se o problema é o microfone, a permissão ou o app.
 */
@Composable
private fun NivelMicrofonePill(
    ouvindo: Boolean,
    hz: Float?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        // As barras de nível: três pulsos, o maior quando há som.
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(6.dp, 12.dp, 18.dp).forEachIndexed { indice, altura ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(altura)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (ouvindo && hz != null) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f + indice * 0.25f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (ouvindo) {
                hz?.let { "${"%.0f".format(it)} Hz" } ?: "-- Hz"
            } else {
                stringResource(R.string.chromatic_mic_off)
            },
            style = DataMonoMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A pastilha de uma corda.
 *
 * Tocar nela toca a nota alvo: é a referência de afinação ao alcance do
 * dedo, sem precisar sair da tela.
 */
@Composable
private fun SeletorDeCorda(
    midi: Int,
    numero: Int,
    ativo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (letra, oitava) = notaEmPartes(midi)
    StudioCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (ativo) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White.copy(alpha = 0.06f)
                },
                shape = MaterialTheme.shapes.medium,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = letra,
                style = MaterialTheme.typography.titleMedium,
                color = if (ativo) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
            )
            Text(
                text = oitava,
                style = DataMonoMedium.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${numero}ª",
                style = LabelCaps.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
    }
}
