package com.afinu.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afinu.app.ui.theme.DataMonoMedium
import com.afinu.app.ui.theme.LabelCaps

/**
 * As peças de interface do Precision Studio Dark.
 *
 * ## Por que não há sombra
 * O design não usa sombra projetada em lugar nenhum. A profundidade vem do
 * **tom**: cada degrau de elevação é um cinza um pouco mais claro que o
 * anterior, e a borda de 1 px com 8% de branco separa um plano do outro.
 * Sombra projetada em fundo escuro aparece como um halo sujo, não como
 * altura.
 *
 * ## A borda
 * `rgba(255,255,255,0.08)` — um fio de luz de meio pixel no alto do cartão.
 * É o que faz o cartão existir sem parecer um adesivo colado na tela.
 */
private val BordaDeCartao = Color.White.copy(alpha = 0.08f)

/**
 * O cartão do design: superfície, borda de 1 px, raio de 16 dp.
 */
@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(BorderStroke(1.dp, BordaDeCartao), MaterialTheme.shapes.large),
    ) {
        conteudo()
    }
}

/**
 * O cartão de ação: um ícone numa caixa, um título e um subtítulo.
 *
 * É a linha de comando do app — "Tocar Cordas", "Filtro de Ruído", "Emitir
 * Tom". O ícone dá a família da ação, o título diz o que é, e o subtítulo
 * diz o que está acontecendo agora.
 */
@Composable
fun StudioActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.secondary,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    StudioCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    // Duas linhas: "Tocar Cordas (Modo Auditivo)" não cabe em
                    // uma, e cortar o nome da ação é pior do que quebrá-lo.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/**
 * A pastilha de estado: um rótulo pequeno dentro de uma cápsula.
 *
 * "EM USO", "OUVINDO", "ATIVO (ESTÚDIO)". É o menor objeto visual do design,
 * e é ele que carrega o estado sem precisar de cor de fundo grande.
 */
@Composable
fun StudioStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    background: Color = color.copy(alpha = 0.16f),
) {
    Text(
        text = text.uppercase(),
        style = LabelCaps.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * O cartão de leitura numérica: um rótulo em cima, um valor grande embaixo.
 *
 * "DESVIO CENTS / 0.0ct" e "FREQUÊNCIA REAL / 82.41Hz". O rótulo é
 * `label-caps` em cinza; o valor é o monoespaçado, que é o que impede o
 * número de pular enquanto ele muda.
 */
@Composable
fun StudioReadoutCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    suffix: String? = null,
) {
    StudioCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = LabelCaps.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = DataMonoMedium,
                    color = valueColor,
                    maxLines = 1,
                )
                if (suffix != null) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = suffix,
                        style = DataMonoMedium.copy(fontSize = DataMonoMedium.fontSize * 0.7f),
                        color = valueColor.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * O interruptor do design: um ponto que acende, não uma caixa que liga.
 *
 * Um interruptor de verdade, de mesa de som, é uma luz. O usuário do app lê
 * "aceso" antes de ler "ligado".
 */
@Composable
fun StudioDotSwitch(
    ligado: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    descricao: String,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (ligado) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            )
            .clickable(onClick = onToggle)
            .semantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) {
        if (ligado) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary),
            )
        }
    }
}

/**
 * O controle segmentado: uma bandeja escura com a pílula que anda por baixo
 * da seleção.
 *
 * É o padrão de hardware — um botão que desliza de posição — e não o padrão
 * de formulário, em que cada opção é um botão independente.
 */
@Composable
fun StudioSegmentedControl(
    opcoes: List<String>,
    selecionada: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icones: List<ImageVector> = emptyList(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        opcoes.forEachIndexed { indice, texto ->
            val ativo = indice == selecionada
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(
                        if (ativo) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable { onSelect(indice) }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (indice < icones.size) {
                    Icon(
                        imageVector = icones[indice],
                        contentDescription = null,
                        tint = if (ativo) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = texto.uppercase(),
                    style = LabelCaps,
                    color = if (ativo) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A marca de "affirmative": um círculo com um visto.
 *
 * Aparece no cartão de cravelha quando a corda está afinada. O verde faz o
 * trabalho; o visto diz o quê, para quem não distingue o verde.
 */
@Composable
fun StudioCheckMark(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size * 0.6f),
        )
    }
}
