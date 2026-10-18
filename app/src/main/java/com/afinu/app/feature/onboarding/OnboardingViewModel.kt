package com.afinu.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afinu.app.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    /** `null` enquanto ainda não sabe se já viu a introdução. */
    val finished: Boolean? = null,
    val page: Int = 0,
) {
    val isFirstPage: Boolean get() = page == 0
    val isLastPage: Boolean get() = page == LAST_PAGE

    companion object {
        const val FIRST_PAGE = 0
        const val LAST_PAGE = 2
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val seen = settingsRepository.settings.first().onboardingSeen
            _uiState.value = OnboardingUiState(finished = seen)
        }
    }

    fun next() {
        _uiState.update { it.copy(page = (it.page + 1).coerceAtMost(OnboardingUiState.LAST_PAGE)) }
    }

    fun back() {
        _uiState.update { it.copy(page = (it.page - 1).coerceAtLeast(OnboardingUiState.FIRST_PAGE)) }
    }

    /**
     * Marca a introdução como vista e libera o app.
     *
     * Fica gravado **antes** de o usuário abrir o afinador: se o app morrer aqui
     * ou o usuário fechar no meio da última página, ele não perde a introdução
     * de novo.
     */
    fun finish() {
        viewModelScope.launch {
            settingsRepository.setOnboardingSeen(true)
            _uiState.value = OnboardingUiState(finished = true)
        }
    }
}