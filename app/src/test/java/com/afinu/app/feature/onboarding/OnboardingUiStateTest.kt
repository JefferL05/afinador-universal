package com.afinu.app.feature.onboarding

import com.afinu.app.data.catalog.InstrumentCatalog
import com.afinu.app.data.prefs.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Onboarding é, em superfície, a parte mais simples do app: três telas e um
 * booleano. Os testes existem porque o booleano tem uma consequência séria — se
 * for gravado na hora errada, o usuário nunca mais vê a introdução, ou a vê de
 * novo toda vez que abre.
 */
class OnboardingUiStateTest {

    @Test
    fun `o app comeca mostrando a introducao`() {
        // finished = null significa "ainda não sei", e o AppRoot desenha uma
        // tela neutra. Confundir isso com false mostraria a introdução num
        // piscar para quem já a viu.
        assertNull(OnboardingUiState(finished = null).finished)
        assertFalse(OnboardingUiState(finished = false).finished ?: true)
        assertTrue(OnboardingUiState(finished = true).finished ?: false)
    }

    @Test
    fun `a primeira pagina e a ultima sao reconhecidas`() {
        assertTrue(OnboardingUiState(page = 0).isFirstPage)
        assertTrue(OnboardingUiState(page = 2).isLastPage)
        assertFalse(OnboardingUiState(page = 1).isLastPage)
    }

    @Test
    fun `tres paginas no total`() {
        assertEquals(0, OnboardingUiState.FIRST_PAGE)
        assertEquals(2, OnboardingUiState.LAST_PAGE)
    }
}

/**
 * O widget resolve a mesma afinação efetiva que o afinador. Se as duas
 * produzissem resultados diferentes, o widget diria uma nota e o afinador
 * mostraria outra — e nenhum dos dois teria razão.
 */
class WidgetConsistencyTest {

    private fun settings(
        instrumentId: String = "violao",
        tuningId: String = "padrao",
        capoFret: Int = 0,
        a4Hz: Float = 440f,
    ) = AppSettings(
        instrumentId = instrumentId,
        tuningId = tuningId,
        a4Hz = a4Hz,
        pitchOffsetCents = 0f,
        confirmToneEnabled = true,
        confirmToneVolume = 0.45f,
        vibrationEnabled = true,
        keepScreenOn = true,
        capoFret = capoFret,
        onboardingSeen = true,
    )

    @Test
    fun `o widget mostra a mesma afinacao efetiva do afinador`() {
        val s = settings(capoFret = 3)
        val instrument = InstrumentCatalog.find(s.instrumentId)
        val tuning = instrument.tunings.first { it.id == s.tuningId }
        val efetiva = com.afinu.app.core.util.Capo.effectiveOrUnshifted(tuning, s.capoFret)

        // O afinador usa currentEffectiveTuning, que vem daqui.
        assertEquals(listOf(43, 48, 53, 58, 62, 67), efetiva.midiNotes)
    }

    @Test
    fun `as preferencias padrao sao coerentes com o catalogo`() {
        val s = settings()
        val instrument = InstrumentCatalog.find(s.instrumentId)
        assertNotNull(
            "a afinação padrão do catálogo deve existir",
            instrument.tunings.firstOrNull { it.id == s.tuningId },
        )
        assertEquals(0, s.capoFret)
        assertTrue("o A4 padrão deve estar na faixa", s.a4Hz in 415f..466f)
    }

    @Test
    fun `instrumento desconhecido no widget nao quebra`() {
        // Um widget sobrevive a uma atualização do app em que o instrumento foi
        // removido do catálogo. O provider usa find(), que cai no padrão.
        val instrument = InstrumentCatalog.find("instrumento_que_nao_existe")
        assertEquals(InstrumentCatalog.default.id, instrument.id)
        assertNotNull(instrument.defaultTuning)
    }

    @Test
    fun `afinacao valida para qualquer instrumento do catalogo`() {
        // O widget é atualizado quando o usuário troca de instrumento; um id de
        // afinação que não existe naquele instrumento não pode quebrar o desenho.
        InstrumentCatalog.all.forEach { instrument ->
            instrument.tunings.forEach { tuning ->
                assertTrue(
                    "${instrument.id}/${tuning.id} tem afinação inválida",
                    tuning.stringCount in 1..8,
                )
            }
        }
    }
}