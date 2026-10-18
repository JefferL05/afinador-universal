package com.afinu.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.afinu.app.MainActivity
import com.afinu.app.R
import com.afinu.app.core.util.Capo
import com.afinu.app.core.util.MusicTheory
import com.afinu.app.data.catalog.InstrumentCatalog
import com.afinu.app.data.prefs.AppSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Widget de referência de afinação.
 *
 * ## O que ele mostra, e o que ele não mostra
 * Ele **não** mostra a nota que está tocando. Para isso seria preciso manter o
 * microfone ligado em segundo plano, e o Android restringe isso a apps de
 * gravação de áudio — o app seria rejeitado na Play Store e, quando permitido,
 * o dreno de bateria o tornaria inútil.
 *
 * O que ele faz é o que ajuda de verdade fora do app: mostra a afinação atual e
 * **toca a referência** ao toque. Afinar "de ouvido" contra a referência do
 * dedo, longe do instrumento, é o caso em que o músico está com o celular no
 * bolso e a mão ocupada — e o widget é a única forma de ter a referência ali.
 *
 * ## Por que redesenha a cada toque
 * `RemoteViews` só pode ser atualizado por `AppWidgetProvider`, e tocar num
 * widget não passa por ele. Um receiver dedicado intercepta o toque, toca o tom e
 * redesenha. Redesenhar do zero garante que a linha que mudou de cor é sempre a
 * que o usuário acabou de ouvir.
 */
@AndroidEntryPoint
class TunerWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var settingsRepository: com.afinu.app.data.prefs.SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        scope.launch {
            val settings = settingsRepository.settings.first()
            appWidgetIds.forEach { id ->
                updateWidget(context, appWidgetManager, id, settings)
            }
        }
    }

    /**
     * Desenha uma instância do widget.
     *
     * Visível para os testes: testar um layout de widget exige um aparelho, e
     * toda a lógica que pode dar errado — resolver afinacao, aplicar capo,
     * escolher quantas linhas mostrar — esta aqui.
     */
    fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        settings: AppSettings,
    ) {
        val instrument = InstrumentCatalog.find(settings.instrumentId)
        val tuning = instrument.tunings.firstOrNull { it.id == settings.tuningId }
            ?: instrument.defaultTuning
        val efetiva = Capo.effectiveOrUnshifted(tuning, settings.capoFret)

        val views = RemoteViews(context.packageName, R.layout.widget_tuning_reference)

        views.setTextViewText(
            R.id.widget_instrument,
            context.getString(
                InstrumentCatalog.displayNameResId(instrument.id),
            ),
        )
        views.setTextViewText(
            R.id.widget_tuning,
            buildString {
                append(tuning.label)
                if (Capo.isInUse(settings.capoFret)) {
                    append(" · ")
                    append(context.getString(R.string.widget_capo, settings.capoFret))
                }
            },
        )
        views.setTextViewText(
            R.id.widget_a4,
            context.getString(R.string.widget_a4, Math.round(settings.a4Hz)),
        )

        // Uma linha por corda, até o limite do layout.
        for (linha in 0 until MAX_LINHAS) {
            val viewId = LINHA_IDS[linha]
            if (linha >= efetiva.stringCount) {
                views.setViewVisibility(viewId, android.view.View.GONE)
                continue
            }
            views.setViewVisibility(viewId, android.view.View.VISIBLE)

            val midi = efetiva.midiNotes[linha]
            // A frequência viaja pronta nos extras, calculada com o A4 do
            // usuário. Assim o tom que soa é exatamente o que a linha mostra.
            val frequencia = MusicTheory.frequencyOf(midi, settings.a4Hz)
            views.setTextViewText(
                viewId,
                context.getString(
                    R.string.widget_string_row,
                    linha + 1,
                    MusicTheory.labelOf(midi),
                ),
            )
            views.setOnClickPendingIntent(
                viewId,
                toneIntent(context, widgetId, linha, frequencia),
            )
        }

        // Tocar no cabeçalho abre o app.
        views.setOnClickPendingIntent(
            R.id.widget_header,
            PendingIntent.getActivity(
                context,
                widgetId,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

        manager.updateAppWidget(widgetId, views)
    }

    /**
     * Toca a nota de referência ao tocar numa linha.
     *
     * O request code inclui o índice da linha **de propósito**: sem isso, duas
     * linhas com a mesma action receberiam o mesmo `PendingIntent` e a segunda
     * tocaria a frequência da primeira. É um bug que só apareceria no aparelho.
     */
    private fun toneIntent(
        context: Context,
        widgetId: Int,
        stringIndex: Int,
        frequencyHz: Float,
    ): PendingIntent {
        val intent = Intent(context, ReferenceToneReceiver::class.java).apply {
            action = ACTION_PLAY_REFERENCE
            putExtra(EXTRA_FREQUENCY_HZ, frequencyHz)
            putExtra(EXTRA_WIDGET_ID, widgetId)
            putExtra(EXTRA_STRING_INDEX, stringIndex)
        }
        return PendingIntent.getBroadcast(
            context,
            widgetId * MAX_LINHAS + stringIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_PLAY_REFERENCE = "com.afinu.app.PLAY_REFERENCE"
        const val EXTRA_FREQUENCY_HZ = "frequency_hz"
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_STRING_INDEX = "string_index"

        /** Máximo de linhas no layout. */
        const val MAX_LINHAS = 6

        val LINHA_IDS = intArrayOf(
            R.id.widget_linha_1,
            R.id.widget_linha_2,
            R.id.widget_linha_3,
            R.id.widget_linha_4,
            R.id.widget_linha_5,
            R.id.widget_linha_6,
        )
    }
}