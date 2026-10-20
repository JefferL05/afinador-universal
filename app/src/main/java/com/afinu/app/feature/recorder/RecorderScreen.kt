package com.afinu.app.feature.recorder

import android.content.ActivityNotFoundException
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gravador de ideias.
 *
 * ## Por que gravar existe em um app de afinador
 * A melodia nasce de improvisar sobre uma afinação. Se o app afina e o
 * musician tem uma frase na cabeça, o lugar natural para ela é o mesmo app: uma
 * aba a mais e a referência de afinação fica do lado.
 *
 * ## Os arquivos não vão para fora
 * Tudo fica em `filesDir`, que é interno do app. Nada aparece na galeria nem
 * em outro app, e apagar o app apaga as gravações. É o que permite ao app não
 * declarar nenhuma permissão de armazenamento — coerente com a promessa de não
 * enviar nada para lugar nenhum.
 */
@Composable
fun RecorderScreen(
    modifier: Modifier = Modifier,
    viewModel: RecorderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Tocar uma gravação abre OUTRO app — o afinador não tem player próprio.
    // O intent é montado no ViewModel (que sabe o FileProvider), mas quem
    // chama `startActivity` é aqui, que é o único com `Context`.
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.recorder),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(24.dp))

        RecordingButton(
            isRecording = state.isRecording,
            elapsedMs = state.elapsedMs,
            onClick = viewModel::toggle,
        )

        Spacer(Modifier.height(8.dp))

        // O botão já mostra o cronômetro enquanto grava, então aqui vai a dica de
        // repouso. Depois de parar, o RESULTADO — que antes ficava guardado num
        // campo de texto que nenhuma tela desenhava.
        val (texto, cor) = when {
            state.isRecording -> {
                stringResource(R.string.recorder_hint) to
                    MaterialTheme.colorScheme.onSurfaceVariant
            }

            // `secondary` é o verde do tema (TunedGreen / TunedGreenDark),
            // então funciona no claro e no escuro sem cor fixa.
            state.result == RecordingResult.SALVO -> {
                stringResource(R.string.recorder_saved) to
                    MaterialTheme.colorScheme.secondary
            }

            state.result == RecordingResult.FALHOU -> {
                stringResource(R.string.recorder_failed) to
                    MaterialTheme.colorScheme.error
            }

            state.result == RecordingResult.SEM_PLAYER -> {
                stringResource(R.string.recorder_no_player) to
                    MaterialTheme.colorScheme.error
            }

            else -> {
                stringResource(R.string.recorder_hint) to
                    MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = cor,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        if (state.recordings.isEmpty()) {
            Text(
                text = stringResource(R.string.recorder_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.recordings, key = { it.absolutePath }) { arquivo ->
                    RecordingRow(
                        file = arquivo,
                        onPlay = {
                            // O toque agora CHEGA a algum lugar: antes o intent
                            // era montado e guardado num campo que ninguém lia.
                            // `ActivityNotFoundException` acontece quando nenhum
                            // app do aparelho assume `audio/*` — não é erro
                            // nosso, mas sem aviso o toque parece não registrado.
                            val intent = viewModel.playIntent(arquivo)
                            try {
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                viewModel.avisoSemPlayer()
                            }
                        },
                        onDelete = { viewModel.delete(arquivo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingButton(
    isRecording: Boolean,
    elapsedMs: Long,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val descricao = stringResource(
            if (isRecording) R.string.recorder_stop else R.string.recorder_start
        )
        Card(
            onClick = onClick,
            shape = CircleShape,
            modifier = Modifier
                .size(96.dp)
                .semantics { contentDescription = descricao },
            colors = CardDefaults.cardColors(
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        if (isRecording) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatarTempo(elapsedMs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun RecordingRow(
    file: File,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onPlay,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = formatarData(file.lastModified()),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${file.length() / 1024} kB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.recorder_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** "m:ss" — acima de uma hora vira "h:mm:ss". */
fun formatarTempo(ms: Long): String {
    val total = ms / 1000
    val horas = total / 3600
    val minutos = (total % 3600) / 60
    val segundos = total % 60
    return if (horas > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", horas, minutos, segundos)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutos, segundos)
    }
}

private fun formatarData(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(epochMs))