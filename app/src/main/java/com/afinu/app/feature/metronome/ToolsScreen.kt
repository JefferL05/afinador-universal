package com.afinu.app.feature.metronome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R
import com.afinu.app.core.audio.Waveform
import com.afinu.app.ui.components.StudioCard
import com.afinu.app.ui.components.StudioSegmentedControl
import com.afinu.app.ui.components.StudioStatusPill
import com.afinu.app.ui.theme.DataMonoLarge
import com.afinu.app.ui.theme.DataMonoMedium
import com.afinu.app.ui.theme.LabelCaps
import kotlin.math.cos
import kotlin.math.sin

/**
 * As ferramentas: metrônomo, tom de referência e gravação.
 *
 * ## Por que nesta ordem
 * O metrônomo vem primeiro porque é o que se usa com mais frequência depois
 * do afinador. O tom de referência é o par do metrônomo: um dá o tempo, o
 * outro dá a altura. A gravação vem por último porque é a que se usa uma
 * vez e esquece.
 */
@Composable
fun ToolsScreen(
    modifier: Modifier = Modifier,
    metronomeViewModel: MetronomeViewModel = hiltViewModel(),
    toolsViewModel: ToolsViewModel = hiltViewModel(),
) {
    val metro by metronomeViewModel.uiState.collectAsStateWithLifecycle()
    val onda by toolsViewModel.waveform.collectAsStateWithLifecycle()
    val a4Hz by toolsViewModel.a4Hz.collectAsStateWithLifecycle()
    var secao by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        StudioSegmentedControl(
            opcoes = listOf(
                stringResource(R.string.tools_metronome),
                stringResource(R.string.tools_tones),
                stringResource(R.string.tools_recorder),
            ),
            selecionada = secao,
            onSelect = { secao = it },
        )

        Spacer(Modifier.height(18.dp))

        // ## Por que cada seção rola por si só
        // O gravador tem uma LazyColumn, e a tela de tom tem rolagem própria.
        // Aninhar qualquer uma delas dentro de uma Column com verticalScroll
        // dá ao Compose uma altura infinita, e ele derruba o app com
        // "vertically scrollable component was measured with an infinity
        // maximum height constraints".
        //
        // Então o contêiner de fora não rola: quem rola é a seção.
        when (secao) {
            1 -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                SecaoTons(
                    onda = onda,
                    a4Hz = a4Hz,
                    onWaveform = toolsViewModel::setWaveform,
                    onTocarNota = toolsViewModel::tocarNota,
                )
            }

            2 -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                com.afinu.app.feature.recorder.RecorderScreen(modifier = Modifier.fillMaxSize())
            }

            else -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                SecaoMetronomo(
                    state = metro,
                    onBpm = metronomeViewModel::setBpm,
                    onTap = metronomeViewModel::tapTempo,
                    onCompasso = metronomeViewModel::setBeatsPerBar,
                    onDivisao = metronomeViewModel::setSubdivisions,
                    onToggle = metronomeViewModel::toggle,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * O metrônomo: o mostrador circular e os controles.
 */
@Composable
private fun SecaoMetronomo(
    state: MetronomeUiState,
    onBpm: (Float) -> Unit,
    onTap: () -> Unit,
    onCompasso: (Int) -> Unit,
    onDivisao: (Int) -> Unit,
    onToggle: () -> Unit,
) {
    // O mostrador: um círculo de transporte, como o de um metrônomo mecânico.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        MostradorDoMetronomo(
            bpm = state.config.bpm,
            batidas = state.config.beatsPerBar,
            emRepouso = !state.isRunning,
            modifier = Modifier.fillMaxSize(),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StudioStatusPill(
                text = stringResource(R.string.tools_tempo_moderate),
                color = MaterialTheme.colorScheme.secondary,
                background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.config.bpm.toInt().toString(),
                    style = DataMonoLarge.copy(fontSize = 44.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.tools_bpm),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            // As batidas do compasso: a primeira acesa, o resto apagado.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(state.config.beatsPerBar.coerceAtMost(8)) { indice ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (indice == 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                },
                            ),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.tools_signature,
                    state.config.beatsPerBar,
                    divisorDeSubdivisao(state.config.subdivisionsPerBar),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(18.dp))

    // Os ajustes de andamento: -5, -1, o "tap tempo", +1, +5.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BotaoDePasso("-5", Modifier.weight(1f)) { onBpm(state.config.bpm - 5f) }
        BotaoDePasso("-1", Modifier.weight(1f)) { onBpm(state.config.bpm - 1f) }
        Box(
            modifier = Modifier
                .weight(2f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onTap)
                .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.tools_tap).uppercase(),
                    style = LabelCaps.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.tools_tap_tempo).uppercase(),
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
        BotaoDePasso("+1", Modifier.weight(1f)) { onBpm(state.config.bpm + 1f) }
        BotaoDePasso("+5", Modifier.weight(1f)) { onBpm(state.config.bpm + 5f) }
    }

    Spacer(Modifier.height(16.dp))

    // A fórmula do compasso.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.tools_signature_label).uppercase(),
            style = LabelCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(2, 3, 4, 6).forEach { tempos ->
                OpcaoRedonda(
                    texto = "$tempos/4",
                    ativo = state.config.beatsPerBar == tempos,
                    onClick = { onCompasso(tempos) },
                )
            }
        }
    }

    Spacer(Modifier.height(10.dp))

    // A divisão.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.tools_division_label).uppercase(),
            style = LabelCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // A fração é o que se vê, e o que se mexe é a subdivisão por
            // tempo. Num compasso 4/4, uma subdivisão por tempo dá 1/4, duas
            // dão 1/8, três dão 1/12 e quatro dão 1/16.
            listOf(1, 2, 3, 4).forEach { porTempo ->
                OpcaoRedonda(
                    texto = "1/${4 * porTempo}",
                    ativo = state.config.subdivisionsPerBeat == porTempo,
                    onClick = { onDivisao(porTempo) },
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    // O botão que liga.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (state.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(
                    if (state.isRunning) R.string.tools_stop else R.string.tools_start,
                ).uppercase(),
                style = LabelCaps,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/**
 * O mostrador circular: o arco verde do pêndulo e os traços do entorno.
 *
 * O arco verde é a **posição do pêndulo**, e ele se move com o andamento.
 * Num metrônomo mecânico é o pêndulo que anda; aqui é o arco, e ele dá a
 * mesma informação: em que parte do ciclo estamos.
 */
@Composable
private fun MostradorDoMetronomo(
    bpm: Float,
    batidas: Int,
    emRepouso: Boolean,
    modifier: Modifier = Modifier,
) {
    val corDoArco = MaterialTheme.colorScheme.primary
    val corDoTraco = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val centro = Offset(size.width / 2f, size.height / 2f)
        val raio = size.minDimension * 0.46f

        // O anel de traços.
        val tracos = 72
        for (i in 0 until tracos) {
            val angulo = Math.toRadians(i * 360.0 / tracos)
            val maior = i % 6 == 0
            val rExterno = raio * (if (maior) 0.90f else 0.95f)
            val rInterno = raio * 0.99f
            drawLine(
                color = if (maior) corDoTraco else corDoTraco.copy(alpha = 0.5f),
                start = Offset(
                    centro.x + (rExterno * cos(angulo)).toFloat(),
                    centro.y + (rExterno * sin(angulo)).toFloat(),
                ),
                end = Offset(
                    centro.x + (rInterno * cos(angulo)).toFloat(),
                    centro.y + (rInterno * sin(angulo)).toFloat(),
                ),
                strokeWidth = if (maior) 2.5.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // O arco do pêndulo: a fração do ciclo que o andamento representa.
        // Ele é verde mesmo em repouso — é a escala do mostrador, não o
        // indicador de que está tocando. O que muda é o quanto do arco
        // está aceso.
        val fracao = ((bpm - MetronomeConfig.MIN_BPM) /
            (MetronomeConfig.MAX_BPM - MetronomeConfig.MIN_BPM)).toFloat().coerceIn(0f, 1f)
        drawArc(
            color = corDoArco.copy(alpha = if (emRepouso) 0.8f else 1f),
            startAngle = -90f,
            sweepAngle = 360f * fracao,
            useCenter = false,
            topLeft = Offset(centro.x - raio * 0.88f, centro.y - raio * 0.88f),
            size = Size(raio * 1.76f, raio * 1.76f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
        )

        // O anel de fundo, para o arco ter onde assentar.
        drawArc(
            color = corDoTraco.copy(alpha = 0.35f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(centro.x - raio * 0.88f, centro.y - raio * 0.88f),
            size = Size(raio * 1.76f, raio * 1.76f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/**
 * O tom de referência: a altura, a forma de onda e as notas de fábrica.
 */
@Composable
private fun SecaoTons(
    onda: Waveform,
    a4Hz: Float,
    onWaveform: (Waveform) -> Unit,
    onTocarNota: (Int) -> Unit,
) {
    StudioCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tools_reference_tone),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.tools_reference_tone_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "%.1f", a4Hz),
                        style = DataMonoLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = stringResource(R.string.tools_hertz).uppercase(),
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // As formas de onda.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Waveform.SINE to R.string.tools_wave_sine,
                    Waveform.TRIANGLE to R.string.tools_wave_triangle,
                    Waveform.SQUARE to R.string.tools_wave_square,
                ).forEach { (forma, rotulo) ->
                    Text(
                        text = stringResource(rotulo).uppercase(),
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = if (forma == onda) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (forma == onda) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                            )
                            .clickable { onWaveform(forma) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onTocarNota(69) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.tools_hear).uppercase(),
                            style = LabelCaps.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // As notas de fábrica: as que o músico usa para calibrar o ouvido.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(60 to "C4", 62 to "D4", 64 to "E4", 67 to "G4", 69 to "A4").forEach { (midi, nome) ->
                    Text(
                        text = nome,
                        style = DataMonoMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { onTocarNota(midi) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}

/** Um botão de passo do andamento: "-5", "+1". */
@Composable
private fun BotaoDePasso(
    texto: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            style = DataMonoMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** Uma opção redonda: a fórmula do compasso, a divisão. */
@Composable
private fun OpcaoRedonda(
    texto: String,
    ativo: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = texto,
        style = LabelCaps.copy(fontSize = 10.sp),
        color = if (ativo) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (ativo) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/**
 * O nome da divisão, como o design escreve.
 *
 * O argumento é a divisão **por compasso**: 4 é semínima, 8 é colcheia.
 * [MetronomeConfig.subdivisionsPerBeat] sozinho não diz nada disso — com 1
 * subdivisão por tempo, o compasso 4/4 tem 4 divisões, e é esse 4 que vira
 * "1/4 Semínima".
 */
private fun divisorDeSubdivisao(subdivisoesPorCompasso: Int): String = when (subdivisoesPorCompasso) {
    4 -> "Semínima"
    8 -> "Colcheia"
    12 -> "Terça"
    16 -> "Semicolcheia"
    else -> "1/${subdivisoesPorCompasso}"
}
