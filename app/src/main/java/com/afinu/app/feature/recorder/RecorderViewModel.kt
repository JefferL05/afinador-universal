package com.afinu.app.feature.recorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecorderUiState(
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0,
    val recordings: List<File> = emptyList(),
    val message: String = "",
) {
    val canRecord: Boolean get() = !isRecording
}

/**
 * Gravador de ideias.
 *
 * ## Por que o tempo é um contador, e não o MediaRecorder
 * `MediaRecorder` tem um relógio próprio, mas ele só é confiável se ninguém
 * interromper a gravação. Um contador em `viewModelScope` continua andando
 * mesmo quando o app vai para segundo plano por um segundo, que é o caso
 * comum quando o usuário troca de aba para ver o próprio braço.
 *
 * ## Limite de duração
 * [IdeaRecorder] define um teto de 10 minutos. Estourar isso enche o
 * armazenamento do usuário sem ele pedir, e uma "ideia" de 40 minutos e
 * um ensaio inteiro, nao uma ideia.
 */
@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val recorder: IdeaRecorder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

    private var elapsedJob: Job? = null

    init {
        recarregar()
    }

    fun toggle() {
        if (_uiState.value.isRecording) parar() else iniciar()
    }

    private fun iniciar() {
        val arquivo = recorder.start()
        if (arquivo == null) {
            _uiState.update {
                it.copy(message = MESSAGE_ERROR, isRecording = false)
            }
            return
        }

        _uiState.update {
            it.copy(isRecording = true, elapsedMs = 0, message = "")
        }

        elapsedJob?.cancel()
        elapsedJob = viewModelScope.launch {
            while (true) {
                delay(200)
                if (!_uiState.value.isRecording) break
                _uiState.update { estado ->
                    estado.copy(elapsedMs = recorder.state.value.let { s ->
                        if (s is RecordingState.Recording) s.elapsedMs else estado.elapsedMs
                    })
                }
            }
        }
    }

    private fun parar() {
        val arquivo = recorder.stop()
        elapsedJob?.cancel()
        elapsedJob = null
        recarregar()
        _uiState.update {
            it.copy(
                isRecording = false,
                elapsedMs = 0,
                message = if (arquivo != null) MESSAGE_OK else MESSAGE_ERROR,
            )
        }
    }

    fun delete(file: File) {
        recorder.deleteRecording(file)
        recarregar()
    }

    fun play(file: File) {
        // A reprodução é delegada ao player do sistema: o app não implementa
        // player, o que significaria lidar com faixas, foco de áudio e o
        // botão do sistema de agora há pouco.
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.fromFile(file),
        ).apply {
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.android.apps.music")
        }
        _uiState.update { it.copy(message = file.name) }
        lastPlayIntent = intent
    }

    /** O ViewModel guarda o intent; quem dispara é a Activity. */
    var lastPlayIntent: android.content.Intent? = null
        private set

    private fun recarregar() {
        _uiState.update { it.copy(recordings = recorder.listRecordings()) }
    }

    override fun onCleared() {
        // Sair da tela cancela a gravação em vez de deixar o microfone ligado.
        recorder.cancel()
        elapsedJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val MESSAGE_OK = "ok"
        const val MESSAGE_ERROR = "erro"
    }
}