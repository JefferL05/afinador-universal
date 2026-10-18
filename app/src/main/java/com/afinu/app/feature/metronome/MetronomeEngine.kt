package com.afinu.app.feature.metronome

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.Arrays
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado observável do metrônomo, para a interface reagir.
 *
 * É separado do motor para que a UI possa mostrar "batendo agora" sem fazer
 * polling a cada quadro.
 */
data class MetronomeTick(
    /** Índice da subdivisão que acabou de soar, dentro do compasso. */
    val subdivisionIndex: Int,
    val beatIndex: Int,
    val isAccent: Boolean,
    /** Tempo do compasso atual, para exibir 1|2|3|4. */
    val barBeat: Int,
)

/**
 * Motor do metrônomo: um único `AudioTrack` contínuo com os cliques escritos
 * nas posições exatas de amostra.
 *
 * ## Como o tempo é garantido
 * O `AudioTrack` em `MODE_STREAM` consome amostras a 48 000 Hz garantidos pelo
 * cristal do aparelho. Cada clique é escrito exatamente
 * `framesPerSubdivision` amostras depois do anterior — medido a partir do início
 * do compasso, nunca acumulado (ver [MetronomeTiming]). O espaçamento entre
 * cliques é portanto exato por construção.
 *
 * ## Por que não usar AudioTrack.MODE_STATIC recarregado a cada batida
 * Recarregar e reiniciar o `AudioTrack` a cada batida introduz uma lacuna de
 * alguns milissegundos entre o fim de um clique e o começo do próximo. Em 7/8
 * isso é um arrasto audível. Aqui o `AudioTrack` **nunca para**: só o conteúdo
 * do buffer muda, e o silêncio entre cliques é preenchido com zeros.
 *
 * ## Latência
 * O buffer do hardware tem [BUFFER_FRAMES] amostras, o que dá ~10 ms em 48 kHz.
 * Abaixo disso o `write` passa a bloquear esperando o dispositivo; acima, a
 * primeira batida demora mais para soar. 10 ms é o ponto em que o atraso é
 * imperceptível e a escrita nunca bloqueia.
 *
 * A instância NÃO é segura para uso concorrente.
 */
@Singleton
class MetronomeEngine @Inject constructor() {

    companion object {
        private const val TAG = "MetronomeEngine"

        const val SAMPLE_RATE = 48_000

        /** ~10 ms de latência. Abaixo disso o write bloqueia; acima, atrasa. */
        const val BUFFER_FRAMES = 512

        /** Duração do clique normal, em ms. Curto o bastante para não cobrir a batida. */
        const val CLICK_MS = 30

        /** O acento é mais agudo **e** mais grave: dá uma "batida dupla" reconhecível. */
        const val ACCENT_MS = 45

        private const val ACCENT_HZ = 1_880f
        private const val NORMAL_HZ = 1_320f

        private const val CLICK_AMPLITUDE = 0.55f
        private const val ACCENT_AMPLITUDE = 0.75f
    }

    private var track: AudioTrack? = null
    private var writerJob: Thread? = null

    private var config: MetronomeConfig = MetronomeConfig()

    /** Total de amostras já escritas no fluxo. Âncora de todo o cálculo. */
    private var framesWritten = 0L

    /**
     * Quantas subdivisões já tocaram desde o início.
     *
     * A posição de cada clique é recalculada a partir deste contador
     * ([MetronomeTiming.absoluteSubdivisionFrame]), nunca somando um passo
     * inteiro a cada clique. Somar acumula o erro de arredondamento e o
     * metrônomo deriva 262 ms em 10 minutos — medido, não estimado.
     */
    private var subdivisionCounter = 0L

    /** Cache dos cliques pré-calculados: só são regerados quando a duração muda. */
    private var normalClick: ShortArray = ShortArray(0)
    private var accentClick: ShortArray = ShortArray(0)

    @Volatile
    private var rodando = false

    /** Chamado a cada clique, na thread de áudio. Nunca bloquear aqui. */
    @Volatile
    var onTick: ((MetronomeTick) -> Unit)? = null

    val isRunning: Boolean get() = rodando

    /**
     * Inicia o metrônomo. Chamar de novo com o já rodando apenas troca a
     * configuração, sem reiniciar a contagem — assim o músico não perde a
     * referência de tempo ao mexer no BPM.
     */
    @Synchronized
    fun start(novaConfig: MetronomeConfig) {
        config = novaConfig
        if (rodando) return

        val tamanhoBytes = BUFFER_FRAMES * 2
        val novo = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
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
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (novo.state != AudioTrack.STATE_INITIALIZED) {
            Log.w(TAG, "AudioTrack não inicializou; o metrônomo não vai soar")
            novo.release()
            return
        }

        track = novo
        framesWritten = 0L
        subdivisionCounter = 0L
        rodando = true
        regenerarCliques()

        novo.play()
        val thread = Thread({ loop(novo) }, "metronome-writer")
        thread.priority = Thread.MAX_PRIORITY
        thread.isDaemon = true
        thread.start()
        writerJob = thread
    }

    /** Troca só o BPM/configuração sem reiniciar a contagem. */
    @Synchronized
    fun updateConfig(novaConfig: MetronomeConfig) {
        config = novaConfig
        if (rodando) regenerarCliques()
    }

    @Synchronized
    fun stop() {
        if (!rodando) return
        rodando = false
        writerJob?.interrupt()
        writerJob?.join(500)
        writerJob = null
        track?.let {
            try {
                if (it.state == AudioTrack.STATE_INITIALIZED) {
                    it.pause()
                    it.flush()
                    it.stop()
                }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Erro ao parar o metrônomo", e)
            }
            it.release()
        }
        track = null
        onTick = null
    }

    /**
     * Posição da próxima subdivisão em amostras do fluxo.
     *
     * Exposta para os testes: é a afirmação central do projeto — o
     * espaçamento é constante e não acumula.
     */
    fun nextSubdivisionFrame(): Long =
        MetronomeTiming.absoluteSubdivisionFrame(subdivisionCounter, config, SAMPLE_RATE)

    /** Subdivisões já tocaram. */
    fun elapsedSubdivisions(): Long = subdivisionCounter

    private fun regenerarCliques() {
        normalClick = gerarClique(NORMAL_HZ, CLICK_MS, CLICK_AMPLITUDE)
        accentClick = gerarClique(ACCENT_HZ, ACCENT_MS, ACCENT_AMPLITUDE)
    }

    /**
     * Gera um clique com envelope exponencial.
     *
     * A senóide pura começa e termina em amplitude cheia e produz um clique
     * (estalo) exatamente nos bordos, que o ouvido pega como ruído de chave. O
     * decaimento de ~8 ms elimina isso.
     */
    private fun gerarClique(frequencia: Float, duracaoMs: Int, amplitude: Float): ShortArray {
        val amostras = (SAMPLE_RATE * duracaoMs / 1000f).roundToInt().coerceAtLeast(8)
        val pico = (amplitude * Short.MAX_VALUE).roundToInt()
        val passo = 2.0 * PI * frequencia / SAMPLE_RATE
        val tau = amostras / 6.0

        return ShortArray(amostras) { i ->
            val envelope = exp(-i / tau)
            (sin(passo * i) * pico * envelope).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    /**
     * Laço de escrita.
     *
     * Never blocks: [AudioTrack.write] retorna o número de amostras aceitas. O
     * laço devolve o que não coube para a próxima volta, o que é o que mantém o
     * metrônomo estável quando a thread é preemptada.
     */
    private fun loop(t: AudioTrack) {
        val buffer = ShortArray(BUFFER_FRAMES)
        while (rodando) {
            Arrays.fill(buffer, 0, BUFFER_FRAMES, 0.toShort())
            quantosCliquesCabem(buffer)

            val escritos = try {
                t.write(buffer, 0, BUFFER_FRAMES)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Escrita interrompida", e)
                break
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Escrita rejeitada", e)
                break
            }

            if (escritos < 0) {
                Log.w(TAG, "AudioTrack.write retornou $escritos")
                break
            }
            framesWritten += escritos
        }
        rodando = false
    }

    /**
     * Escreve todos os cliques que caem dentro dos próximos [BUFFER_FRAMES]
     * amostras, e avança o relógio.
     *
     * A posição de cada clique é sempre
     * `round(contador × amostrasPorSubdivisãoIdeal)`. Nunca
     * `posiçãoAtual + amostrasPorSubdivisão`: essa forma carrega um erro de
     * arredondamento por clique e faz a peça inteira derivar.
     */
    private fun quantosCliquesCabem(buffer: ShortArray) {
        val fimDaJanela = framesWritten + BUFFER_FRAMES

        // A 300 BPM com 8 subdivisões são 1500 amostras por clique, então cabe
        // 0 ou 1 por janela de 512. Ainda assim é um while: com sampleRate
        // menor ou mais subdivisões cabem vários, e o laço não pode assumir.
        while (true) {
            val alvo = nextSubdivisionFrame()
            if (alvo >= fimDaJanela) break

            val deslocamento = alvo - framesWritten
            if (deslocamento >= 0) {
                val indice = deslocamento.toInt()
                val acento = MetronomeTiming.isAccent(subdivisionCounter, config)
                copiar(buffer, indice, if (acento) accentClick else normalClick)

                onTick?.invoke(
                    MetronomeTick(
                        subdivisionIndex = MetronomeTiming.localIndexInBar(subdivisionCounter, config),
                        beatIndex = MetronomeTiming.localBeatInBar(subdivisionCounter, config),
                        isAccent = acento,
                        barBeat = MetronomeTiming.localBeatInBar(subdivisionCounter, config) + 1,
                    )
                )
            }

            subdivisionCounter++
        }
    }

    /** Copia um clique no buffer, truncando o que passar do fim da janela. */
    private fun copiar(destino: ShortArray, inicio: Int, conteudo: ShortArray) {
        if (inicio >= destino.size) return
        val quantos = minOf(destino.size - inicio, conteudo.size)
        if (quantos <= 0) return
        System.arraycopy(conteudo, 0, destino, inicio, quantos)
    }
}