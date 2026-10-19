package com.afinu.app.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captura o microfone em blocos, sem alocar nada por leitura.
 *
 * ## Por que UNPROCESSED primeiro
 * `AudioSource.UNPROCESSED` entrega o sinal cru, sem o processador de voz do
 * fabricante. Em aparelhos com agressor de ruído para chamadas, as outras
 * fontes aplicam filtro passa-banda e eco que **distorcem a frequência
 * fundamental** — o afinador passa a ler errado em justamente graves e agudos.
 * Nem todo aparelho suporta UNPROCESSED, daí a cadeia de tentativas.
 */
@Singleton
class AudioCapture @Inject constructor() {

    companion object {
        const val SAMPLE_RATE = 48_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        private const val TAG = "AudioCapture"

        private val SOURCE_PRIORITY = intArrayOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
        )
    }

    private var record: AudioRecord? = null
    // Reatribuidos quando o tamanho do quadro muda (troca de instrumento).
    private var shortBuffer: ShortArray = ShortArray(0)
    private var floatBuffer: FloatArray = FloatArray(0)

    /** Fonte efetivamente usada, útil para diagnóstico em ajustes. */
    var activeSource: Int = -1
        private set

    val isRunning: Boolean
        get() = record?.let {
            it.recordingState == AudioRecord.RECORDSTATE_RECORDING
        } ?: false

    /**
     * Prepara e inicia a captura.
     *
     * @param frameSize amostras por leitura. Deve ser múltiplo do tamanho de
     *   janela que o detector vai usar, para não descascar amostras.
     * @return true se a captura começou.
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(frameSize: Int): Boolean {
        stop()
        require(frameSize > 0) { "frameSize inválido: $frameSize" }

        if (shortBuffer.size != frameSize) {
            shortBuffer = ShortArray(frameSize)
            floatBuffer = FloatArray(frameSize)
        }

        for (source in SOURCE_PRIORITY) {
            val criado = tryCreate(source, frameSize)
            if (criado != null) {
                record = criado
                activeSource = source
                return try {
                    criado.startRecording()
                    criado.recordingState == AudioRecord.RECORDSTATE_RECORDING
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "Falha ao iniciar gravação na fonte $source", e)
                    criado.release()
                    record = null
                    false
                }
            }
        }
        Log.e(TAG, "Nenhuma fonte de áudio pôde ser aberta")
        return false
    }

    private fun tryCreate(source: Int, frameSize: Int): AudioRecord? {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING)
        if (minBuffer <= 0) {
            Log.w(TAG, "getMinBufferSize retornou $minBuffer para a fonte $source")
            return null
        }
        // Buffer do hardware 4x o quadro: absorve picos de agendamento do SO sem
        // aumentar a latência percebida (a latência é o tamanho do quadro, não
        // do buffer do hardware).
        val bufferBytes = maxOf(minBuffer, frameSize * BYTES_PER_SHORT * 4)
        return try {
            AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(ENCODING)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .build()
        } catch (e: UnsupportedOperationException) {
            Log.w(TAG, "Fonte de áudio $source não suportada", e)
            null
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Parâmetros inválidos para a fonte de áudio $source", e)
            null
        } catch (e: SecurityException) {
            // A permissão é concedida em tempo de execução. Se o usuário a
            // revogar nas configurações entre a checagem e esta chamada — ou
            // se a política do fabricante empurrar a decisão para cá — isto
            // estoura. Sem esta captura o app fecha em vez de mostrar
            // "microfone indisponível".
            Log.w(TAG, "Sem permissão de gravação para a fonte $source", e)
            null
        }
    }

    /**
     * Lê um quadro de amostras normalizado em [-1, 1].
     *
     * Devolve o buffer interno (sem cópia). O chamador deve consumir os dados
     * antes da próxima chamada. Retorna null se a leitura falhar ou o
     * dispositivo for desconectado no meio da sessão.
     */
    fun readFrame(): FloatArray? {
        val atual = record ?: return null
        val lidos = try {
            atual.read(shortBuffer, 0, shortBuffer.size)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Leitura interrompida — microfone em uso por outro app", e)
            return null
        }

        if (lidos <= 0) {
            // ERROR_INVALID_OPERATION (-3) ou ERROR_BAD_VALUE (-2) indicam
            // dispositivo perdido; nesse caso o app deve parar e avisar.
            if (lidos < 0) Log.w(TAG, "AudioRecord.read retornou $lidos")
            return null
        }

        // 32768 e não 32767: é o que mapeia -32768 para -1.0 exato. Dividir por
        // 32767 jogaria o pico para -1.00003, fora da faixa que o detector
        // assume e capaz de produzir um clique no registro mais grave.
        val escala = 1f / 32768f
        for (i in 0 until lidos) {
            floatBuffer[i] = shortBuffer[i] * escala
        }
        // Zera a cauda do quadro anterior para não alimentar o detector com
        // amostras velhas quando a leitura vier parcial.
        if (lidos < floatBuffer.size) {
            floatBuffer.fill(0f, lidos, floatBuffer.size)
        }
        return floatBuffer
    }

    /**
     * Libera o microfone.
     *
     * Não pular esta chamada deixa o ícone de microfone do sistema ligado e
     * impede outros apps de usar o áudio — o que faz o app ser rejeitado em
     * revisão de loja.
     */
    fun stop() {
        record?.let {
            try {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Erro ao parar gravação", e)
            }
            it.release()
        }
        record = null
        activeSource = -1
    }
}

private const val BYTES_PER_SHORT = 2