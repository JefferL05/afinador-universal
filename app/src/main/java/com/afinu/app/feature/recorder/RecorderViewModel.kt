package com.afinu.app.feature.recorder

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    /**
     * Resultado da última tentativa de gravação, para a tela confirmar.
     *
     * Antes isto era `message: String`, com os sentinemas `"ok"` e `"erro"`,
     * e a tela só lia o campo enquanto `isRecording` era `true` — justamente
     * o momento em que ele valia `""`. Ou seja: a mensagem de sucesso ou de
     * falha existia no estado e nunca chegava a ser desenhada. Parar uma
     * gravação não confirmava nada, e uma falha passava em silêncio.
     */
    val result: RecordingResult? = null,
) {
    val canRecord: Boolean get() = !isRecording
}

/**
 * Resultado de uma tentativa de gravação.
 *
 * Motivo tipado e não texto, pela mesma razão dos falhos do afinador: o
 * ViewModel não tem `Context` para localizar, e a frase pronta em português
 * apareceria igual em inglês e em espanhol. A tela resolve para o idioma.
 */
enum class RecordingResult {
    /** O arquivo existe e tem tamanho maior que zero. */
    SALVO,

    /** O `MediaRecorder` falhou ao iniciar, ou parou sem produzir amostra. */
    FALHOU,

    /** Nada no aparelho sabe tocar áudio — o toque na gravação foi recusado. */
    SEM_PLAYER,
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
    @ApplicationContext private val context: Context,
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
            // Falhou antes de gravar uma única amostra. O usuário precisa ver
            // isso: o botão volta ao estado "gravar" e, sem aviso, parece que
            // o toque não foi registrado.
            _uiState.update {
                it.copy(result = RecordingResult.FALHOU, isRecording = false)
            }
            return
        }

        _uiState.update {
            it.copy(isRecording = true, elapsedMs = 0, result = null)
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
        // `stop()` devolve `null` quando nada foi gravado — `MediaRecorder`
        // interrompido, ou um arquivo truncado de zero bytes. Antes isso virava
        // um sentinema "erro" que ninguém desenhava.
        _uiState.update {
            it.copy(
                isRecording = false,
                elapsedMs = 0,
                result = if (arquivo != null) {
                    RecordingResult.SALVO
                } else {
                    RecordingResult.FALHOU
                },
            )
        }
    }

    fun delete(file: File) {
        recorder.deleteRecording(file)
        recarregar()
    }

    /**
     * Intent que abre a gravação no player do sistema.
     *
     * ## Por que virou função, e não um campo
     * Antes o intent era montado e guardado em `lastPlayIntent`, mas **ninguém**
     * chamava `startActivity`: tocar numa gravação não fazia literalmente nada,
     * sem erro e sem mensagem. A Activity não é alcançável daqui, e o
     * Composable tem o `Context` na mão — então a função devolve o intent e quem
     * dispara é a tela.
     *
     * O `FileProvider` dá um `content://` no lugar do `Uri.fromFile`: a partir
     * do Android 7 mandar `file://` para outro processo lança
     * `FileUriExposedException` e o intent morre no caminho.
     *
     * Não há `resolveActivity()` aqui de propósito: ele devolve `null` no
     * Android 11+ salvo se houver `<queries>` declarada, o que transformaria
     * todo toque em "sem player" num aparelho que tem um. Quem dispara pega a
     * `ActivityNotFoundException`.
     */
    fun playIntent(file: File): android.content.Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "audio/*")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** A tela chama quando `startActivity` lança `ActivityNotFoundException`. */
    fun avisoSemPlayer() {
        _uiState.update { it.copy(result = RecordingResult.SEM_PLAYER) }
    }

    private fun recarregar() {
        _uiState.update { it.copy(recordings = recorder.listRecordings()) }
    }

    override fun onCleared() {
        // Sair da tela cancela a gravação em vez de deixar o microfone ligado.
        recorder.cancel()
        elapsedJob?.cancel()
        super.onCleared()
    }
}