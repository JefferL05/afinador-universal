package com.afinu.app.feature.tuner

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afinu.app.core.audio.AudioCapture
import com.afinu.app.core.audio.ConfirmTonePlayer
import com.afinu.app.core.audio.NoteMapper
import com.afinu.app.core.audio.PitchSmoother
import com.afinu.app.core.audio.YinDetector
import com.afinu.app.core.model.Instrument
import com.afinu.app.core.model.InstrumentFamily
import com.afinu.app.core.model.TuningPreset
import com.afinu.app.core.util.Capo
import com.afinu.app.data.catalog.InstrumentCatalog
import com.afinu.app.data.prefs.AppSettings
import com.afinu.app.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Orquestra a tela de afinador: captura -> detecção -> suavização -> estado.
 *
 * ## Três decisões que definem a sensação do app
 *
 * 1. **A leitura só chega ao estado depois da suavização.** O detector dispara
 *    ~12x por segundo; sem o filtro, a agulha treme e o usuário não consegue
 *    ler nada.
 *
 * 2. **O bip de confirmação dispara na entrada da zona, não enquanto nela.**
 *    Se tocar continuamente, o afinador vira um apito que não cala.
 *
 * 3. **Trocar de instrumento reinicia a captura**, porque a janela de análise
 *    muda de 4096 para 8192 conforme o instrumento mais grave.
 */
@HiltViewModel
class TunerViewModel @Inject constructor(
    private val audioCapture: AudioCapture,
    private val settingsRepository: SettingsRepository,
    private val confirmTone: ConfirmTonePlayer,
    private val haptics: Haptics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private val smoother = PitchSmoother()

    private var detector: YinDetector? = null
    private var activeWindow = 0
    private var audioJob: Job? = null

    private var currentInstrument: Instrument = InstrumentCatalog.default
    private var currentTuning: TuningPreset = InstrumentCatalog.default.defaultTuning
    private var settings: AppSettings? = null

    /**
     * Afinação com o capo aplicado. É o alvo do medidor: com capo na 3ª casa,
     * a 6ª corda afinada em E2 soa G2, e é com G2 que o músico compara.
     */
    private var currentEffectiveTuning: TuningPreset = InstrumentCatalog.default.defaultTuning

    /** A última leitura estava dentro da zona? Controla a borda do bip. */
    private var wasInTune = false

    /** A última nota exibida era esta? Controla o "note just changed". */
    private var lastNoteKey: String? = null

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                // Só a troca de instrumento reinicia a captura, porque é a
                // única coisa que muda o tamanho da janela de análise. Trocar de
                // afinação ou de capo altera o alvo do medidor e sai de graça.
                val mudouInstrumento = s.instrumentId != currentInstrument.id
                applySettings(s)

                if (audioJob != null && mudouInstrumento) {
                    stopAudio()
                    startAudio(hasPermission = true)
                } else if (audioJob == null) {
                    startAudio(hasPermission = _uiState.value.hasPermission)
                }
            }
        }
    }

    private fun applySettings(s: AppSettings) {
        val instrument = InstrumentCatalog.find(s.instrumentId)
        val tuning = instrument.tunings.firstOrNull { it.id == s.tuningId }
            ?: instrument.defaultTuning

        currentInstrument = instrument
        currentTuning = tuning
        settings = s
        currentEffectiveTuning = Capo.effectiveOrUnshifted(tuning, s.capoFret)

        val window = YinDetector.windowFor(instrument.needsDeepWindow)
        if (window != activeWindow) {
            activeWindow = window
            detector = YinDetector(
                sampleRate = AudioCapture.SAMPLE_RATE,
                windowSize = window,
            )
            // A mediana guardada foi medida com outra janela; descartar evita
            // uma leitura errada logo após a troca.
            smoother.reset()
            wasInTune = false
        }

        _uiState.update {
            it.copy(
                instrument = instrument,
                tuning = tuning,
                effectiveTuning = currentEffectiveTuning,
                capoFret = s.capoFret,
                a4Hz = s.a4Hz,
                pitchOffsetCents = s.pitchOffsetCents,
                confirmToneEnabled = s.confirmToneEnabled,
                confirmToneVolume = s.confirmToneVolume,
                vibrationEnabled = s.vibrationEnabled,
            )
        }
    }

    /** Chamado quando a permissão de microfone muda. */
    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            startAudio(hasPermission = true)
        } else {
            stopAudio()
            _uiState.update { it.copy(micState = MicState.NeedsPermission, reading = null) }
        }
    }

    fun startAudioIfPermitted(hasPermission: Boolean) {
        if (!hasPermission) {
            _uiState.update { it.copy(micState = MicState.NeedsPermission) }
            return
        }
        if (audioJob?.isActive == true) return
        startAudio(hasPermission = true)
    }

    @SuppressLint("MissingPermission") // A permissão é verificada por quem chama.
    private fun startAudio(hasPermission: Boolean) {
        if (!hasPermission) return
        if (activeWindow == 0) return

        val frameSize = activeWindow
        val started = audioCapture.start(frameSize)
        if (!started) {
            _uiState.update {
                it.copy(micState = MicState.Failed("Não foi possível acessar o microfone"))
            }
            return
        }

        _uiState.update { it.copy(micState = MicState.Listening) }

        audioJob?.cancel()
        audioJob = viewModelScope.launch(Dispatchers.IO) {
            while (currentCoroutineContext().isActive) {
                val frame = audioCapture.readFrame() ?: run {
                    if (!audioCapture.isRunning) {
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(micState = MicState.Failed("Microfone interrompido"))
                            }
                        }
                        return@launch
                    }
                    continue
                }
                processFrame(frame)
            }
        }
    }

    private fun stopAudio() {
        audioJob?.cancel()
        audioJob = null
        audioCapture.stop()
        smoother.reset()
        wasInTune = false
        _uiState.update {
            it.copy(micState = MicState.Idle, reading = null, detectedStringIndex = null)
        }
    }

    private fun processFrame(frame: FloatArray) {
        val det = detector ?: return
        val s = settings ?: return

        val estimate = det.detect(frame)
        val rawFrequency = if (estimate != null && estimate.confidence >= TunerLogic.MIN_CONFIDENCE) {
            estimate.frequencyHz
        } else {
            null
        }

        val smoothed = smoother.accept(rawFrequency, s.a4Hz)
        val reading = smoothed?.let { NoteMapper.map(it, s.a4Hz, s.pitchOffsetCents) }

        val stringIndex = if (reading != null &&
            currentInstrument.family == InstrumentFamily.STRINGS
        ) {
            TunerLogic.detectString(currentEffectiveTuning, reading.frequencyHz, s.a4Hz)
        } else {
            null
        }

        val inTune = reading?.inTune == true
        val enteredTune = inTune && !wasInTune
        wasInTune = inTune

        val noteKey = reading?.key
        val changed = noteKey != null && noteKey != lastNoteKey
        if (noteKey != null) lastNoteKey = noteKey

        // enteredTune implica reading != null, mas o compilador não deduz isso.
        val confirmed = reading?.takeIf { enteredTune }
        if (confirmed != null) {
            if (s.confirmToneEnabled) {
                confirmTone.play(
                    frequencyHz = confirmed.targetFrequencyHz,
                    volume = s.confirmToneVolume,
                )
            }
            if (s.vibrationEnabled) haptics.inTune()
        } else if (changed && s.vibrationEnabled) {
            haptics.tick()
        }

        val guided = _uiState.value.guidedStringIndex
        val advanced = if (TunerLogic.shouldAdvanceGuided(guided, stringIndex, inTune)) {
            val next = guided + 1
            if (next < currentEffectiveTuning.midiNotes.size) next else null
        } else {
            guided
        }

        _uiState.update {
            it.copy(
                reading = reading,
                smoothedFrequencyHz = smoothed,
                detectedStringIndex = stringIndex,
                noteJustChanged = changed,
                justGotInTune = enteredTune,
                guidedStringIndex = advanced,
                inTuneCount = if (enteredTune) it.inTuneCount + 1 else it.inTuneCount,
            )
        }
    }

    // --- Ações do usuário ------------------------------------------------------

    fun selectInstrument(id: String) {
        viewModelScope.launch { settingsRepository.setInstrument(id) }
    }

    fun selectTuning(id: String) {
        viewModelScope.launch { settingsRepository.setTuning(id) }
    }

    /** Trocar o capo reexibe a afinação efetiva; não exige reiniciar o áudio. */
    fun setCapoFret(fret: Int) {
        viewModelScope.launch { settingsRepository.setCapoFret(fret) }
    }

    fun setA4(hz: Float) {
        viewModelScope.launch { settingsRepository.setA4(hz) }
    }

    fun setPitchOffset(cents: Float) {
        viewModelScope.launch { settingsRepository.setPitchOffset(cents) }
    }

    /** Reinicia o modo guiado para a primeira corda. */
    fun restartGuidedMode() {
        _uiState.update { it.copy(guidedStringIndex = 0) }
    }

    fun stopGuidedMode() {
        _uiState.update { it.copy(guidedStringIndex = null) }
    }

    override fun onCleared() {
        // Sem isto o ícone de microfone do sistema fica ligado depois de sair.
        audioCapture.stop()
        confirmTone.release()
        super.onCleared()
    }
}