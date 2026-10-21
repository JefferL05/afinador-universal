package com.afinu.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.afinu.app.core.audio.ConfirmTonePlayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Recebe o toque numa linha do widget e toca a nota de referência.
 *
 * ## Por que um receiver separado
 * `AppWidgetProvider.onReceive` não sabe qual linha foi tocada — ele só recebe
 * o widget. Separar os dois deixa o `TunerWidgetProvider` responsável apenas por
 * desenhar, e este receiver só por soar.
 *
 * ## Por que a frequência vem pronta nos extras
 * Ler as preferências aqui exigiria DataStore a partir de um `BroadcastReceiver`,
 * que não tem escopo de corrotina garantido — e traria uma corrida: o usuário
 * poderia mudar a afinação entre o desenho e o toque, e o widget soaria uma nota
 * diferente da que mostra.
 *
 * A frequência ja foi calculada com o A4 do usuário na hora de desenhar, então
 * ela viaja pronta. O widget e o som **nao podem divergir**: ver a linha marcada
 * como "Do" e ouvir Sol, fora de sintonia, seria o app se contradizindo.
 */
@AndroidEntryPoint
class ReferenceToneReceiver : BroadcastReceiver() {

    @Inject
    lateinit var tonePlayer: ConfirmTonePlayer

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TunerWidgetProvider.ACTION_PLAY_REFERENCE) return

        val frequencia = intent.getFloatExtra(TunerWidgetProvider.EXTRA_FREQUENCY_HZ, 0f)
        if (!frequencia.isFinite() || frequencia <= 0f) {
            Log.w("ReferenceTone", "Frequencia invalida nos extras: $frequencia")
            return
        }

        // 1,2 s: tempo suficiente para afinar contra a referência e curto o
        // bastante para o musician não perder o compasso enquanto ela soa.
        tonePlayer.play(
            frequencyHz = frequencia,
            durationMs = TONE_DURATION_MS,
            volume = TONE_VOLUME,
        )

        // O player é uma instância por dono — não é singleton — então quem
        // toca é quem solta. Sem esta liberação, cada toque do widget deixaria
        // um AudioTrack vivo no processo.
        Handler(Looper.getMainLooper()).postDelayed(
            { tonePlayer.release() },
            TONE_DURATION_MS + 200L,
        )
    }

    private companion object {
        const val TONE_DURATION_MS = 1_200
        const val TONE_VOLUME = 0.6f
    }
}