package com.afinu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afinu.app.ui.navigation.Destination
import com.afinu.app.ui.theme.LabelCaps

/**
 * A barra de navegação.
 *
 * ## Por que não é a `NavigationBar` do Material
 * Porque a `NavigationBar` desenha o item selecionado como uma cápsula
 * arredondada — o modelo de "pílula" do Material 3. O design pede um fio
 * verde de 2 px no topo do item, que é a marca de um equipamento: o item
 * ativo é iluminado, não destacado.
 *
 * ## Por que a barra é translúcida
 * Ela flutua sobre o conteúdo, e o conteúdo passa por baixo dela. Com uma
 * superfície opaca, uma lista rolando embaixo pareceria ter sido cortada.
 */
@Composable
fun TuneMasterBottomBar(
    atual: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Destination.entries.forEach { destino ->
                BottomBarItem(
                    destino = destino,
                    selecionado = destino == atual,
                    onClick = { onSelect(destino) },
                )
            }
        }
        // A barra do sistema entra abaixo da nossa, e o conteúdo scrolla por
        // baixo dela. Sem isto, o último item fica sob o botão do Android.
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun BottomBarItem(
    destino: Destination,
    selecionado: Boolean,
    onClick: () -> Unit,
) {
    val interacao = remember { MutableInteractionSource() }
    val cor = if (selecionado) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val rotulo = stringResource(destino.labelRes)

    Column(
        modifier = Modifier
            .width(80.dp)
            .height(52.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(
                interactionSource = interacao,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics {
                selected = selecionado
                contentDescription = rotulo
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // O fio do item ativo: 2 px no topo, largura do item.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    if (selecionado) MaterialTheme.colorScheme.primary else Color.Transparent,
                ),
        )
        Spacer(Modifier.height(4.dp))
        Icon(
            imageVector = destino.icon,
            contentDescription = null,
            tint = cor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = rotulo,
            style = LabelCaps.copy(fontSize = 10.sp),
            color = cor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
