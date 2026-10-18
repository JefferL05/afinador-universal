package com.afinu.app.feature.tuner

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.afinu.app.core.audio.NoteReading
import com.afinu.app.core.model.TuningPreset
import com.afinu.app.core.util.MusicTheory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Resposta tátil curta ao acertar a nota.
 *
 * Isolar o vibrador aqui evita que o ViewModel precise lidar com as duas APIs
 * (API 31+ usa VibratorManager, abaixo usa o serviço legado) e mantém o
 * ViewModel testável em JVM puro.
 */
@Singleton
class Haptics @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun tick() = vibrate(longArrayOf(0, 18))

    fun inTune() = vibrate(longArrayOf(0, 25, 60, 35))

    private fun vibrate(pattern: LongArray) {
        val vibrator = vibrator() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Alguns aparelhos sem vibrador lançam aqui. Não é motivo para
            // derrubar o app: a confirmação sonora e visual já bastam.
            Log.w("Haptics", "Vibração indisponível", e)
        }
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
}

/** Resultado da análise de um quadro de áudio. */
internal data class FrameAnalysis(
    val reading: NoteReading?,
    val smoothedFrequencyHz: Float?,
    val stringIndex: Int?,
    val enteredTune: Boolean,
)

/**
 * Regras de decisão puras do afinador, isoladas de Android.
 *
 * Separar isto do ViewModel é o que permite testar "qual corda está tocando"
 * com números, em vez de depender do microfone de um emulador.
 */
internal object TunerLogic {

    /** Confiança mínima para aceitar uma leitura. Abaixo disso é ruído. */
    const val MIN_CONFIDENCE = 0.80f

    /**
     * Encontra a corda mais provável comparando **cents**, não notas.
     *
     * Escolher a nota MIDI mais próxima dá a resposta errada com frequência:
     * no violão, o intervalo da 3ª para a 4ª corda é de 4 semitons, mas o da
     * 4ª para a 5ª é de 5. Dois tons a meia distância de cada nota ficam
     * ambíguos, e o cents resolve isso porque mede a distância real no
     * círculo dos tons.
     */
    fun detectString(tuning: TuningPreset, frequencyHz: Float, a4Hz: Float): Int? {
        if (tuning.midiNotes.isEmpty()) return null
        if (!frequencyHz.isFinite() || frequencyHz <= 0f) return null
        return tuning.midiNotes.indices.minByOrNull { index ->
            val alvo = MusicTheory.frequencyOf(tuning.midiNotes[index], a4Hz)
            abs(MusicTheory.centsBetween(frequencyHz, alvo))
        }
    }

    /** Desvio em cents entre a frequência medida e a da corda indicada. */
    fun centsToString(tuning: TuningPreset, index: Int, frequencyHz: Float, a4Hz: Float): Float {
        require(index in tuning.midiNotes.indices) { "Índice de corda inválido: $index" }
        val alvo = MusicTheory.frequencyOf(tuning.midiNotes[index], a4Hz)
        return MusicTheory.centsBetween(frequencyHz, alvo)
    }

    /**
     * Modo guiado: avança para a próxima corda só quando a corda pedida foi
     * afinada de fato. Avançar só por "detectou a corda errada" faz o app
     * pular cordas sem o usuário ter afinado nada.
     */
    fun shouldAdvanceGuided(
        guidedIndex: Int?,
        detectedIndex: Int?,
        inTune: Boolean,
    ): Boolean = guidedIndex != null && detectedIndex == guidedIndex && inTune
}
