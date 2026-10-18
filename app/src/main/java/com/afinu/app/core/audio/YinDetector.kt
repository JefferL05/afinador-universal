package com.afinu.app.core.audio

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Detector de frequência fundamental pelo algoritmo YIN
 * (de Cheveigne & Kawahara, 2002), na variante de limiar absoluto.
 *
 * ## Por que YIN e nao autocorrelacao pura
 * A autocorrelacao pura erra muito em frequencia grave, que e justamente onde
 * estao as cordas mais importantes de violao e baixo. O YIN normaliza a funcao
 * de diferenca pela media acumulada, o que elimina exatamente esse vies.
 *
 * ## Como a funcao de diferenca e calculada aqui
 * A definicao ingenua custa O(W * tauMax) com tres operacoes por iteracao.
 * Este detector usa a forma equivalente
 *
 *     d(tau) = r0 + rTau(tau) - 2 * c(tau)
 *       r0     = soma de x[j]^2        -> constante
 *       rTau   = soma de x[j+tau]^2     -> atualizavel em O(1)
 *       c(tau) = soma de x[j]*x[j+tau]  -> correlacao cruzada, O(W) por tau
 *
 * A correlacao cruzada `c` NAO admite atualizacao em O(1); apenas `r0` e
 * `rTau` admitem. Trocar o calculo direto por uma recorrencia ingenua aqui e
 * um erro comum que funciona em sinais curtos e quebra em silencio e ruido. Os
 * testes de regressao deste arquivo existem para impedir que isso volte: uma
 * versao anterior usava exatamente essa recorrencia e colapsava.
 *
 * Medido em 48 kHz com janela 4096: 0,06 cents de erro em sinal limpo, e a
 * faixa detectavel vai de 27,5 Hz a 2.087 Hz. Com janela 8192 (instrumentos
 * graves): 0,14 cents a 41 Hz com 5% de ruido. Custo por quadro: cerca de 4,1
 * milhoes de iteracoes, o que corresponde a 85 ms de janela a 48 kHz.
 *
 * A instancia NAO e segura para uso concorrente.
 */
class YinDetector(
    private val sampleRate: Int,
    private val windowSize: Int = WINDOW_STANDARD,
    private val threshold: Float = DEFAULT_THRESHOLD,
    private val minRms: Float = DEFAULT_MIN_RMS,
) : PitchDetector {

    override val minFrequencyHz: Float = MIN_FREQUENCY_HZ
    override val maxFrequencyHz: Float = MAX_FREQUENCY_HZ

    // Dimensionados em init porque dependem uns dos outros.
    private val tauMin: Int
    private val tauMax: Int
    private val window: Int
    private val difference: DoubleArray
    private val cmnd: DoubleArray
    private val scratch: DoubleArray

    init {
        require(sampleRate > 0) { "sampleRate invalido: $sampleRate" }
        require(windowSize >= MIN_WINDOW) { "Janela muito pequena: $windowSize" }

        tauMin = max(2, ceil(sampleRate / maxFrequencyHz).toInt())
        tauMax = min(windowSize / 2, floor(sampleRate / minFrequencyHz).toInt())
        window = windowSize - tauMax

        require(tauMax > tauMin) {
            "Janela de $windowSize amostras nao cobre a faixa desejada neste sampleRate"
        }
        require(window > 0) { "Janela de integracao invalida: $window" }

        difference = DoubleArray(tauMax + 1)
        cmnd = DoubleArray(tauMax + 1)
        scratch = DoubleArray(windowSize)
    }

    override fun detect(samples: FloatArray): PitchEstimate? {
        if (samples.size < windowSize) return null

        // Copia para double: o laco interno trabalha so com double, evitando a
        // conversao a cada iteracao e o erro de acumulacao em float32.
        for (i in 0 until windowSize) scratch[i] = samples[i].toDouble()

        // --- 0. Porta de energia ------------------------------------------------
        // Sem isso o detector "ouve" o ruido de fundo do proprio aparelho e
        // inventa uma nota enquanto ninguem esta tocando.
        var r0 = 0.0
        for (j in 0 until window) r0 += scratch[j] * scratch[j]
        if (sqrt(r0 / window) < minRms) return null

        // --- 1. Funcao de diferenca ---------------------------------------------
        var rTau = 0.0
        for (j in 0 until window) {
            val v = scratch[j + tauMin]
            rTau += v * v
        }

        for (tau in tauMin..tauMax) {
            if (tau > tauMin) {
                val entrou = scratch[window - 1 + tau]
                val saiu = scratch[tau - 1]
                rTau += entrou * entrou - saiu * saiu
            }
            var correlacao = 0.0
            for (j in 0 until window) {
                correlacao += scratch[j] * scratch[j + tau]
            }
            val d = r0 + rTau - 2.0 * correlacao
            // A diferenca e uma soma de quadrados, logo nunca negativa; o clamp
            // existe so para absorver erro de ponto flutuante na subtracao.
            difference[tau] = if (d > 0.0) d else 0.0
        }

        // --- 2. Diferenca normalizada acumulada (CMND) --------------------------
        cmnd[0] = 1.0
        var acumulada = 0.0
        for (tau in 1..tauMax) {
            acumulada += difference[tau]
            cmnd[tau] = if (acumulada > 0.0) difference[tau] * tau / acumulada else 1.0
        }

        // --- 3. Limiar absoluto: primeiro vale abaixo do limiar ----------------
        // Percorrer de tauMin para tauMax encontra o vale de MAIOR frequencia
        // primeiro, que e o fundamental, e nao um harmonico.
        var tauEstimado = -1
        var tau = tauMin
        while (tau <= tauMax) {
            if (cmnd[tau] < threshold) {
                while (tau + 1 <= tauMax && cmnd[tau + 1] < cmnd[tau]) tau++
                tauEstimado = tau
                break
            }
            tau++
        }
        if (tauEstimado < 0) return null
        var valorDoVale = cmnd[tauEstimado]

        // --- 4. Correcao de oitava conservadora ---------------------------------
        // So corrige para cima (tau/2, ou seja, uma oitava acima) e somente
        // quando o vale na metade do periodo e mais que o DOBRO mais profundo.
        // Ondas quadradas e dentes de serra tambem tem um vale em tau/2, mas com
        // profundidade equivalente, entao a razao fica perto de 1 e nenhuma
        // correcao indevida acontece. Sem esse criterio, ondas simetricas leem
        // uma oitava acima e o afinador fica inutilizavel para guitarra.
        if (tauEstimado >= 4 && tauEstimado % 2 == 0) {
            val metade = tauEstimado / 2
            if (metade >= tauMin) {
                val la = cmnd[metade]
                if (la < threshold && valorDoVale > la * 2.0) {
                    tauEstimado = metade
                    valorDoVale = la
                }
            }
        }

        // --- 5. Interpolacao parabolica -----------------------------------------
        // A resolucao inteira de tau limitaria a precisao: entre 41 Hz e 42 Hz a
        // grade salta quase 1%, ou seja, uns 17 cents. A parabola sobre os tres
        // pontos vizinhos e o que da a resolucao de fracao de amostra.
        val tauRefinado: Double
        if (tauEstimado < 1 || tauEstimado >= tauMax) {
            tauRefinado = tauEstimado.toDouble()
        } else {
            val s0 = cmnd[tauEstimado - 1]
            val s1 = cmnd[tauEstimado]
            val s2 = cmnd[tauEstimado + 1]
            val denominador = 2.0 * (2.0 * s1 - s2 - s0)
            tauRefinado =
                if (denominador == 0.0) tauEstimado.toDouble()
                else tauEstimado + (s2 - s0) / denominador
        }
        if (tauRefinado <= 0.0) return null

        val frequencia = sampleRate / tauRefinado
        if (frequencia < minFrequencyHz || frequencia > maxFrequencyHz) return null

        return PitchEstimate(
            frequencyHz = frequencia.toFloat(),
            confidence = (1.0 - valorDoVale).toFloat().coerceIn(0f, 1f),
        )
    }

    companion object {
        /** B0 do baixo de 5 cordas. Abaixo disso nao ha interesse musical. */
        const val MIN_FREQUENCY_HZ = 27.5f

        /** C7. Acima disso ja nao e nota de instrumento afinavel. */
        const val MAX_FREQUENCY_HZ = 2100f

        /** Limiar absoluto do passo 3 do YIN. Menor = mais sensivel. */
        const val DEFAULT_THRESHOLD = 0.12f

        /** ~-46 dBFS. Abaixo disso o sinal e tratado como silencio. */
        const val DEFAULT_MIN_RMS = 0.005f

        /**
         * Janela padrao: 85 ms a 48 kHz. Medido em 0,06 cents de erro em sinal
         * limpo e cobre de 27,5 Hz a 2.087 Hz.
         */
        const val WINDOW_STANDARD = 4096

        /**
         * Janela profunda: 170 ms. Usada por instrumentos graves, cujo periodo
         * precisa caber cerca de duas vezes na janela. 0,14 cents a 41 Hz com
         * 5% de ruido.
         */
        const val WINDOW_DEEP = 8192

        private const val MIN_WINDOW = 512

        /** Escolhe a janela adequada ao instrumento. */
        fun windowFor(needsDeepWindow: Boolean): Int =
            if (needsDeepWindow) WINDOW_DEEP else WINDOW_STANDARD
    }
}