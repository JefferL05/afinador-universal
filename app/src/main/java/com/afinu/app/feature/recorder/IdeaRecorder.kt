package com.afinu.app.feature.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado da gravação.
 */
sealed interface RecordingState {
    data object Idle : RecordingState

    /** Gravando há [elapsedMs]. */
    data class Recording(val elapsedMs: Long) : RecordingState

    /** Convertendo para um arquivo finalizável. */
    data object Saving : RecordingState
}

/**
 * Grava ideias: melodia, cifra, progressão.
 *
 * ## Por que MediaRecorder e não o nosso próprio AudioRecord
 * Escrever WAVPCM à mão é fácil, mas é AudioRecord + um arquivo em crescimento,
 * e um crash no meio deixa o arquivo truncado sem cabeçalho — o player não
 * abre. `MediaRecorder` grava direto para um container válido, fecha o arquivo
 * ao parar e tem o `onError` do framework. Para o uso real (gravar uma ideia e
 * ouvir 10 minutos depois) isso é mais robusto do que parece.
 *
 * ## Onde o áudio fica
 * Em `filesDir`, que é interno do app. Nada vai para a galeria, nada aparece
 * para outros apps e a limpeza é automática quando o usuário desinstala — o que
 * também significa que um app de tuner sem permissão de armazenamento não
 * precisa declarar `READ/WRITE_EXTERNAL_STORAGE`.
 */
@Singleton
class IdeaRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "IdeaRecorder"

        /** Formato AAC: ~1 MB por minuto, qualidade mais que suficiente para uma ideia. */
        private const val BIT_RATE = 128_000
        private const val SAMPLE_RATE = 44_100

        private const val MAX_DURATION_MS = 10 * 60 * 1000L

        /** Subpasta de `filesDir` onde ficam os arquivos. */
        private const val DIR_GRAVACOES = "ideias"
    }

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startedAtEpochMs = 0L

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    val isRecording: Boolean get() = _state.value is RecordingState.Recording

    /** Duração máxima antes de parar sozinho, para não estourar o armazenamento. */
    val maxDurationMs: Long get() = MAX_DURATION_MS

    /**
     * Começa a gravar. Devolve o arquivo, ou null se não foi possível abrir o
     * microfone.
     */
    fun start(): File? {
        if (isRecording) stop()

        val arquivo = File(
            File(context.filesDir, DIR_GRAVACOES),
            "ideia_${System.currentTimeMillis()}.m4a",
        )
        arquivo.parentFile?.mkdirs()

        val rec = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Não foi possível criar o MediaRecorder", e)
            return null
        }

        try {
            rec.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioSamplingRate(SAMPLE_RATE)
            rec.setAudioEncodingBitRate(BIT_RATE)
            rec.setMaxDuration(MAX_DURATION_MS)
            rec.setOutputFile(arquivo.absolutePath)
            rec.prepare()
            rec.start()
        } catch (e: Exception) {
            // Um `prepare` falha quando o microfone está ocupado por outro app.
            // O melhor que dá é limpar e avisar — nunca deixar um arquivo de 0 byte.
            Log.e(TAG, "Falha ao preparar a gravação", e)
            runCatching { rec.release() }
            arquivo.delete()
            _state.value = RecordingState.Idle
            return null
        }

        recorder = rec
        currentFile = arquivo
        startedAtEpochMs = System.currentTimeMillis()
        _state.value = RecordingState.Recording(0)
        return arquivo
    }

    /**
     * Para e devolve o arquivo final.
     *
     * Devolve null se nada foi gravado — um arquivo de 0 byte faz o player
     * falhar na hora de abrir, então é melhor não criá-lo.
     */
    fun stop(): File? {
        val rec = recorder ?: return null
        val arquivo = currentFile

        try {
            rec.stop()
        } catch (e: RuntimeException) {
            // `stop()` lança se gravação nenhuma foi feita, ou se foi
            // interrompida. O arquivo fica truncado e é melhor removê-lo.
            Log.w(TAG, "Gravação interrompida; descartando arquivo", e)
            arquivo?.delete()
            arquivo = null
        } finally {
            runCatching { rec.release() }
            recorder = null
            currentFile = null
        }

        _state.value = RecordingState.Idle
        startedAtEpochMs = 0L
        if (arquivo == null || !arquivo.exists() || arquivo.length() == 0L) {
            arquivo?.delete()
            return null
        }
        return arquivo
    }

    /** Atualiza o tempo decorrido. Chamado pela interface a cada segundo. */
    fun refreshElapsed() {
        val atual = _state.value
        if (atual is RecordingState.Recording) {
            _state.update { RecordingState.Recording(System.currentTimeMillis() - startedAtEpochMs) }
        }
    }

    /** Lista as gravações já feitas, mais recentes primeiro. */
    fun listRecordings(): List<File> {
        val dir = File(context.filesDir, DIR_GRAVACOES)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.name.endsWith(".m4a") && f.length() > 0 }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteRecording(file: File): Boolean = file.delete()

    fun cancel() {
        recorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        recorder = null
        currentFile?.delete()
        currentFile = null
        _state.value = RecordingState.Idle
    }
}