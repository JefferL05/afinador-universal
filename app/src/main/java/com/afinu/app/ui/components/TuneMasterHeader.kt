package com.afinu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afinu.app.ui.theme.DataMonoMedium
import com.afinu.app.ui.theme.LabelCaps
import com.afinu.app.ui.theme.StudioCyan

/**
 * O cabeçalho do app.
 *
 * ## O que ele carrega
 * Marca, a tela em que se está, a referência de altura e um atalho de perfil.
 * Tudo em 64 dp de altura, e nada mais: o cabeçalho é um cartaz, não um
 * painel de controle. Quem está afinando olha para o medidor, não para o
 * topo da tela.
 *
 * ## Por que a referência de altura fica no cabeçalho
 * Porque ela é a coisa que muda de uma sessão para outra e precisa estar
 * visível o tempo todo: quem afina em 432 Hz e ouve 440 Hz no afinador perde
 * o tempo todo. Ver o valor ao lado da marca é o lembrete permanente.
 */
@Composable
fun TuneMasterHeader(
    /** Nome da tela, em maiúsculas: "VIOLÃO", "CROMÁTICO"... */
    section: String,
    /** A4 de referência em uso, em Hertz. */
    a4Hz: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // A barra de status é do sistema e fica por cima do conteúdo: sem
            // isto, o relógio do aparelho atravessa a marca.
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Marca: o símbolo do afinador dentro de um bloco arredondado.
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = StudioCyan,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        // O nome ocupa o que sobrar e cede: sem `weight`, um nome longo
        // empurra a pílula do A4 e o botão de perfil para fora da tela.
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TuneMaster",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                // Selo PRO: um ponto de exclamação, não um elogio.
                Text(
                    text = "PRO",
                    style = LabelCaps.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Text(
                text = section.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Referência de altura, sempre à vista.
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = "A4=${a4Hz.formatAsHertz()}",
                style = DataMonoMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Perfil",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * O valor de referência, como aparece no cabeçalho: "440Hz" ou "432,5Hz".
 *
 * A vírgula é o separador decimal de Portugal e do Brasil. O valor é lido
 * por gente que vai tocar junto, e um "432.5Hz" seria lido como "quatrocentos
 * e trinta e dois vírgula cinco" por um leitor de tela em pt.
 */
private fun Float.formatAsHertz(): String {
    val texto = if (this % 1f == 0f) {
        this.toInt().toString()
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f", this).replace('.', ',')
    }
    return "${texto}Hz"
}
