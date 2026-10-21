package com.afinu.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afinu.app.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * O estado da casca do app.
 *
 * Só o cabeçalho precisa de informação que não é da tela: a referência de
 * altura aparece em todas as abas, e ela vive nas preferências, não no
 * ViewModel do afinador. Sem esta classe, cada aba teria de observar as
 * preferências por conta própria.
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** A4 de referência, em Hertz. */
    val a4Hz: StateFlow<Float> = settingsRepository.settings
        .map { it.a4Hz }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 440f,
        )
}
