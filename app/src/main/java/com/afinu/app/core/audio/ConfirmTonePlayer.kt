package com.afinu.app.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Toca um tom curto na frequência indicada.
 *
 * Usa `MODE_STATIC` com um buffer reaproveitado: o tom tem duração fixa, então
 * só a frequência muda entre chamadas. O envelope de rampa é obrigatório —
 * uma senóide que começa e termina em amplitude cheia produz um clique
 * audível, que é o primeiro defeito que o usuário percebe em um afinador.
 *
 * ## Por que não é um singleton
 * Cada dono de áudio tem a sua instância: o afinador, a tela de acordes e o
 * widget do launcher tocam em contextos diferentes, e uma track compartilhada
 * faria um deles cortar o som do outro no meio — exatamente a "interrupção
 * inesperada" que um app de instrumento não pode ter. O widget, que é um
 * receiver, precisa soltar a sua depois de cada toque.
 */
class ConfirmTonePlayer @Inject constructor() {

    companion object {
        private const val TAG = "ConfirmTone"
        const val SAMPLE_RATE = 48_000
        const val DEFAULT_DURATION_MS = 110

        private const val FADE_MS = 8
    }

    private var track: AudioTrack? = null
    private var pcm: ShortArray = ShortArray(0)

    /**
     * Bytes que a track atual comporta.
     *
     * Uma track em `MODE_STATIC` nasce com o buffer dimensionado e não cresce:
     * escrever mais do que cabe faz o `write` falhar **em silêncio**, e o tom
     * simplesmente não sai. Como o mesmo player toca bip de 110 ms e nota de
     * 1,2 s, a track é refeita quando o tom seguinte não couber.
     */
    private var bufferBytes = 0

    /**
     * Toca um tom curto na frequência indicada.
     *
     * @param volume 0 a 1. O sinal é escalado por esse fator, respeitando o
     *   volume do aparelho (o AudioTrack usa `STREAM_MUSIC`).
     */
    fun play(
        frequencyHz: Float,
        durationMs: Int = DEFAULT_DURATION_MS,
        volume: Float = 0.45f,
    ) {
        if (!frequencyHz.isFinite() || frequencyHz <= 0f) return
        if (durationMs <= 0 || volume <= 0f) return

        val samples = (SAMPLE_RATE * durationMs / 1000f).roundToInt().coerceAtLeast(64)
        if (pcm.size != samples) pcm = ShortArray(samples)

        val fadeSamples = (SAMPLE_RATE * FADE_MS / 1000f).roundToInt().coerceAtLeast(1)
        val amplitude = (volume * Short.MAX_VALUE).roundToInt()
        val passo = 2.0 * PI * frequencyHz / SAMPLE_RATE

        for (i in 0 until samples) {
            val envelope = when {
                i < fadeSamples -> i.toFloat() / fadeSamples
                i > samples - fadeSamples -> (samples - i).toFloat() / fadeSamples
                else -> 1f
            }
            pcm[i] = (sin(passo * i) * amplitude * envelope).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }

        val tamanhoBytes = pcm.size * 2

        try {
            val existente = track
            if (existente != null &&
                existente.state == AudioTrack.STATE_INITIALIZED &&
                bufferBytes >= tamanhoBytes
            ) {
                existente.stop()
                existente.reloadStaticData()
                existente.write(pcm, 0, pcm.size)
                existente.setVolume(volume)
                existente.play()
                return
            }

            // O tom não cabe na track atual: ela é refeita com o buffer novo.
            existente?.release()
            bufferBytes = tamanhoBytes
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(tamanhoBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
                .also {
                    it.write(pcm, 0, pcm.size)
                    it.setVolume(volume)
                    it.play()
                }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Não foi possível tocar o bip de confirmação", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Áudio indisponível para o bip", e)
        }
    }

    fun release() {
        track?.let {
            try {
                if (it.state == AudioTrack.STATE_INITIALIZED) it.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Erro ao parar bip", e)
            }
            it.release()
        }
        track = null
        bufferBytes = 0
    }
}