// FilterChip, Card(onClick) e AssistChip ainda sao experimentais no Material 3.
@file:OptIn(ExperimentalMaterial3Api::class)

package com.afinu.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afinu.app.R
import com.afinu.app.core.model.InstrumentFamily
import com.afinu.app.core.util.Capo
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.data.catalog.InstrumentCatalog
import com.afinu.app.data.repository.FavoriteTuning
import com.afinu.app.ui.components.ChipRow
import com.afinu.app.ui.components.movingSelectedToFront
import kotlin.math.roundToInt

/**
 * Tela de ajustes.
 *
 * ## Organização por tarefa, não por tipo de controle
 * As seções seguem o que o usuário quer ajustar — "que instrumento", "que
 * afinação", "que som" — em vez de agrupar todos os sliders num lugar e todas
 * as listas noutro. Isso importa em tela de celular: procurar "onde mudo o
 * bip" é mais rápido sabendo que ele está junto das coisas de feedback.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var favoriteToDelete by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(16.dp))

        InstrumentSection(state, viewModel)
        SettingsDivider()
        TuningSection(state, viewModel)
        SettingsDivider()
        CapoSection(state, viewModel)
        SettingsDivider()
        ReferenceSection(state, viewModel)
        SettingsDivider()
        FeedbackSection(state, viewModel)
        SettingsDivider()
        FavoritesSection(
            state = state,
            onSave = { showSaveDialog = true },
            onApply = viewModel::applyFavorite,
            onDelete = { favoriteToDelete = it },
            onExport = { showExportDialog = true },
            onImport = { showImportDialog = true },
        )
        SettingsDivider()
        AboutSection()

        Spacer(Modifier.height(32.dp))
    }

    if (showSaveDialog) {
        SaveFavoriteDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { nome ->
                viewModel.saveCurrentAsFavorite(nome)
                showSaveDialog = false
            },
        )
    }

    if (showExportDialog) {
        ExportDialog(
            text = viewModel.exportAsText(),
            onDismiss = { showExportDialog = false },
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = { texto ->
                viewModel.importFromText(texto)
                showImportDialog = false
            },
        )
    }

    favoriteToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { favoriteToDelete = null },
            title = { Text(stringResource(R.string.delete_favorite_title)) },
            text = { Text(stringResource(R.string.delete_favorite_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFavorite(id)
                    favoriteToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { favoriteToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    when (val outcome = state.importOutcome) {
        ImportOutcome.Idle -> Unit
        is ImportOutcome.Success -> ImportBanner(
            text = stringResource(R.string.import_success, outcome.count),
            isError = false,
            onDismiss = viewModel::dismissImportOutcome,
        )
        is ImportOutcome.Failure -> ImportBanner(
            text = stringResource(R.string.import_failure),
            isError = true,
            onDismiss = viewModel::dismissImportOutcome,
        )
    }
}

// ---------------------------------------------------------------------------
// Seções
// ---------------------------------------------------------------------------

@Composable
private fun InstrumentSection(state: SettingsUiState, vm: SettingsViewModel) {
    SettingsGroup(title = stringResource(R.string.instrument)) {
        // Todos os instrumentos, com o atual em primeiro: trocar é raro, e
        // rolagem para achar o atual é o que torna a lista inútil.
        val catalogo = InstrumentCatalog.all.movingSelectedToFront(state.instrument)

        ChipRow(
            items = catalogo,
            selected = state.instrument,
            labelOf = { it.displayName },
            descriptionOf = { "${it.displayName}, ${it.tunings.size} afinações" },
            onSelect = { vm.selectInstrument(it.id) },
        )
    }
}

@Composable
private fun TuningSection(state: SettingsUiState, vm: SettingsViewModel) {
    if (state.instrument.family == InstrumentFamily.FREEFORM) {
        SettingsGroup(title = stringResource(R.string.tuning)) {
            Text(
                text = stringResource(R.string.no_tuning_for_freeform),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    SettingsGroup(title = stringResource(R.string.tuning)) {
        // Linhas verticais aqui, e nao chips: sao ate 12 afinacoes com nomes
        // longos ("Meio tom abaixo"), que nao cabem numa fita horizontal.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.instrument.tunings.forEach { tuning ->
                FilterChip(
                    selected = tuning.id == state.tuning.id,
                    onClick = { vm.selectTuning(tuning.id) },
                    label = { Text(tuning.label) },
                    modifier = Modifier.semantics {
                        contentDescription = "${tuning.label}, " +
                            tuning.midiNotes.joinToString(" ") { MusicTheory.labelOf(it) }
                    },
                )
            }
        }
    }
}

@Composable
private fun CapoSection(state: SettingsUiState, vm: SettingsViewModel) {
    if (state.instrument.family == InstrumentFamily.FREEFORM) return

    SettingsGroup(title = stringResource(R.string.capo)) {
        Text(
            text = stringResource(R.string.capo_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        Capo.frets.forEach { fret ->
            FilterChip(
                selected = state.capoFret == fret,
                onClick = { vm.setCapoFret(fret) },
                label = { Text(if (fret == 0) stringResource(R.string.no_capo) else fret.toString()) },
            )
        }

        Spacer(Modifier.height(12.dp))

        val maisGrave = state.tuning.midiNotes.minOrNull()
        if (maisGrave != null) {
            val aberta = MusicTheory.labelOf(maisGrave)
            val soando = MusicTheory.labelOf(Capo.resultingTonic(state.tuning, state.capoFret))
            Text(
                text = stringResource(R.string.capo_effect, aberta, soando),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceSection(state: SettingsUiState, vm: SettingsViewModel) {
    SettingsGroup(title = stringResource(R.string.reference)) {
        Text(
            text = stringResource(R.string.reference_a4_value, state.a4Hz.toInt()),
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            value = state.a4Hz,
            onValueChange = vm::setA4,
            valueRange = MusicTheory.MIN_A4_HZ..MusicTheory.MAX_A4_HZ,
            // Um passo de 1 Hz por toque de dedo: 415..466 tem 51 valores, que
            // é exatamente o número de posições de um track. Passos menores
            // exigiriam precisão de sub-pixel para ser alcançáveis.
            steps = (MusicTheory.MAX_A4_HZ - MusicTheory.MIN_A4_HZ).roundToInt() - 1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(415, 432, 440, 442, 444, 466).forEach { preset ->
                AssistChip(
                    onClick = { vm.setA4(preset.toFloat()) },
                    label = { Text("$preset") },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pitch_offset),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = state.pitchOffsetHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = vm::resetPitchOffset) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.pitch_offset_reset),
                )
            }
        }
        Slider(
            value = state.pitchOffsetCents,
            onValueChange = vm::setPitchOffset,
            valueRange = -50f..50f,
            steps = 99,
        )
    }
}

@Composable
private fun FeedbackSection(state: SettingsUiState, vm: SettingsViewModel) {
    SettingsGroup(title = stringResource(R.string.feedback)) {
        SwitchRow(
            title = stringResource(R.string.confirm_tone),
            checked = state.confirmToneEnabled,
            onCheckedChange = vm::setConfirmToneEnabled,
        )
        if (state.confirmToneEnabled) {
            Text(
                text = stringResource(R.string.confirm_tone_volume),
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = state.confirmToneVolume,
                onValueChange = vm::setConfirmToneVolume,
                valueRange = 0f..1f,
                steps = 19,
            )
        }
        SwitchRow(
            title = stringResource(R.string.vibration),
            checked = state.vibrationEnabled,
            onCheckedChange = vm::setVibrationEnabled,
        )
        SwitchRow(
            title = stringResource(R.string.keep_screen_on),
            checked = state.keepScreenOn,
            onCheckedChange = vm::setKeepScreenOn,
        )
    }
}

@Composable
private fun FavoritesSection(
    state: SettingsUiState,
    onSave: () -> Unit,
    onApply: (FavoriteTuning) -> Unit,
    onDelete: (Long) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.favorites)) {
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save_current_as_favorite))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.export_favorites))
            }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.import_favorites))
            }
        }

        if (state.favorites.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.no_favorites),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(Modifier.height(12.dp))
            state.favorites.forEach { favorite ->
                FavoriteRow(
                    name = favorite.name,
                    subtitle = favorite.subtitle,
                    stale = favorite.stale,
                    onApply = { onApply(favorite) },
                    onDelete = { onDelete(favorite.id) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AboutSection() {
    SettingsGroup(title = stringResource(R.string.about)) {
        Text(
            text = stringResource(R.string.version, com.afinu.app.BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.privacy_short),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Diálogos
// ---------------------------------------------------------------------------

@Composable
private fun SaveFavoriteDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var nome by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_favorite_title)) },
        text = {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                singleLine = true,
                label = { Text(stringResource(R.string.favorite_name)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nome) },
                enabled = nome.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ExportDialog(text: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_favorites)) },
        text = {
            Text(
                text = stringResource(R.string.export_explainer),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = text, readOnly = true, minLines = 4, maxLines = 10)
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(text))
                onDismiss()
            }) { Text(stringResource(R.string.copy)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var texto by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_favorites)) },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text(stringResource(R.string.paste_json)) },
                minLines = 4,
                maxLines = 10,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(texto) }, enabled = texto.isNotBlank()) {
                Text(stringResource(R.string.import_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ImportBanner(text: String, isError: Boolean, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Componentes
// ---------------------------------------------------------------------------

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Column(content = content)
    }
}

@Composable
private fun SettingsDivider() {
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FavoriteRow(
    name: String,
    subtitle: String?,
    stale: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onApply,
        enabled = !stale,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (stale) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle ?: stringResource(R.string.favorite_stale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}