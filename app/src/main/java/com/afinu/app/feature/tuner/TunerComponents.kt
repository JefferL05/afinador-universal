package com.afinu.app.feature.tuner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afinu.app.core.audio.NoteReading
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.ui.accessibility.TouchTarget
import com.afinu.app.ui.theme.CloseAmber
import com.afinu.app.ui.theme.OffRed
import com.afinu.app.ui.theme.TunedGreen
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Medidor de afinação em arco.
 *
 * ## Por que um arco e não uma barra linear
 * Uma barra horizontal é mais precisa para números; um arco é mais rápido para
 * o olho. Aqui a leitura é "está no centro ou não", não um valor a conferir —
 * então o arco serve melhor. A régua de cents aparece em texto logo abaixo,
 * para quem quiser o número.
 *
 * ## A agulha não é desenhada como triângulo
 * Triângulo com vértice no centro é o desenho padrão, mas ele esconde a
 * posição exata sob a ponta fina. Uma cápsula com borda arredondada de 12 px
 * é mais grossa nas duas pontas, então continua visível quando está em cima do
 * centro — que é justamente o momento que importa.
 */
@Composable
fun NoteDial(
    reading: NoteReading?,
    modifier: Modifier = Modifier,
    /** Avisa o TalkBack que o valor mudou, sem anunciar a cada quadro. */
    accessibilityNote: String? = null,
) {
    val cents = reading?.cents ?: 0f
    val inTune = reading?.inTune == true

    val normalized = (cents / MAX_CENTS).coerceIn(-1f, 1f)
    val target by animateFloatAsState(
        targetValue = normalized,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "agulha",
    )

    val needleColor by animateColorAsState(
        targetValue = when {
            inTune -> TunedGreen
            abs(cents) <= 15f -> CloseAmber
            else -> OffRed
        },
        label = "corDaAgulha",
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val zoneColor = TunedGreen.copy(alpha = 0.28f)

    val description = accessibilityNote ?: reading?.let {
        "${MusicTheory.spokenNameOf(it.midi)}, ${"%.0f".format(it.cents)} cents"
    } ?: "Nenhuma nota detectada"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics(mergeDescendants = true) {
                // stateDescription em vez de contentDescription: o valor muda a
                // cada quadro, e o TalkBack anuncia automaticamente quando um
                // stateDescription muda, sem o usuário navegar até aqui.
                stateDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val strokeWidth = 14.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height - 20.dp.toPx())
            val arcBox = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // Arco de fundo
            drawArc(
                color = trackColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcBox,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Zona verde: os 2 cents de tolerância, desenhados na escala real.
            val inTuneHalfSweep = (SWEEP_ANGLE / 2f) * (IN_TUNE_CENTS / MAX_CENTS)
            drawArc(
                color = zoneColor,
                startAngle = CENTER_ANGLE - inTuneHalfSweep,
                sweepAngle = inTuneHalfSweep * 2f,
                useCenter = false,
                topLeft = topLeft,
                size = arcBox,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Agulha
            val angle = CENTER_ANGLE + target * (SWEEP_ANGLE / 2f)
            val rad = Math.toRadians(angle.toDouble())
            val innerRadius = radius - strokeWidth * 1.6f
            val outerRadius = radius + strokeWidth * 0.4f
            val start = Offset(
                center.x + (innerRadius * cos(rad)).toFloat(),
                center.y + (innerRadius * sin(rad)).toFloat(),
            )
            val end = Offset(
                center.x + (outerRadius * cos(rad)).toFloat(),
                center.y + (outerRadius * sin(rad)).toFloat(),
            )
            drawLine(
                color = needleColor,
                start = start,
                end = end,
                strokeWidth = 7.dp.toPx(),
                cap = StrokeCap.Round,
            )

            // Marca de zero
            drawCircle(
                color = trackColor,
                radius = 3.dp.toPx(),
                center = Offset(
                    center.x + ((radius + strokeWidth * 0.7f) * cos(Math.toRadians(CENTER_ANGLE.toDouble()))).toFloat(),
                    center.y + ((radius + strokeWidth * 0.7f) * sin(Math.toRadians(CENTER_ANGLE.toDouble()))).toFloat(),
                ),
            )
        }
    }
}

/** Valor numérico em cents, com sinal e uma casa decimal. */
@Composable
fun CentsReadout(reading: NoteReading?, modifier: Modifier = Modifier) {
    val text = when {
        reading == null -> "—"
        abs(reading.cents) < 0.05f -> "0.0"
        else -> String.format("%+.1f", reading.cents)
    }
    val color = when {
        reading == null -> MaterialTheme.colorScheme.onSurfaceVariant
        reading.inTune -> TunedGreen
        abs(reading.cents) <= 15f -> CloseAmber
        else -> OffRed
    }
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Medium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier.clearAndSetSemantics {
            stateDescription = "$text cents"
        },
    )
    Text(
        text = "cents",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clearAndSetSemantics { },
    )
}

/**
 * Nota em tamanho gigante.
 *
 * A letra é o que o músico olha primeiro; por isso ela domina a tela e a
 * oitava é um detalhe embaixo. Quando não há leitura, vira um traço em vez de
 * "C4" fixo — mostrar uma nota enquanto ninguém toca é misinformation pior que
 * uma tela vazia.
 */
@Composable
fun NoteReadout(
    reading: NoteReading?,
    stringLabel: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = reading?.letter ?: "—",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Thin,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (reading != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringLabel ?: "oitava ${reading.octave}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Botões de corda.
 *
 * Tocar num botão **não** exige microfone: ele apenas fija qual corda o
 * usuário quer mirar, o que também é útil quando está afinando "de ouvido".
 */
@Composable
fun StringPicker(
    count: Int,
    detectedIndex: Int?,
    guidedIndex: Int?,
    labels: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val isDetected = index == detectedIndex
            val isGuided = index == guidedIndex
            val bg by animateColorAsState(
                targetValue = when {
                    isGuided -> MaterialTheme.colorScheme.primary
                    isDetected -> TunedGreen.copy(alpha = 0.22f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "corDoBotao$index",
            )
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(12.dp),
                color = bg,
                modifier = Modifier
                    .size(TouchTarget.MIN_SIZE)
                    .clearAndSetSemantics {
                        contentDescription = labels.getOrNull(index) ?: "Corda ${index + 1}"
                        // O estado muda sozinho quando o app identifica a
                        // corda, e o TalkBack precisa avisar isso.
                        stateDescription = when {
                            isGuided -> "pedida agora"
                            isDetected -> "detectada"
                            else -> ""
                        }
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isGuided) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Aviso mostrado quando falta a permissão de microfone ou falhou a captura. */
@Composable
fun MicStatusBanner(
    state: MicState,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (message, action) = when (state) {
        MicState.NeedsPermission -> "O afinador precisa do microfone para ouvir o instrumento." to "Permitir"
        is MicState.Failed -> state.reason to "Tentar de novo"
        else -> return
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.error,
            ) {
                Text(
                    action,
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private const val MAX_CENTS = 50f
private const val IN_TUNE_CENTS = NoteReading.IN_TUNE_CENTS
private const val START_ANGLE = 200f
private const val SWEEP_ANGLE = 140f
private const val CENTER_ANGLE = START_ANGLE + SWEEP_ANGLE / 2f