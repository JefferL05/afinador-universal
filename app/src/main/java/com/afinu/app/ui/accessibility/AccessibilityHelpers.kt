package com.afinu.app.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.afinu.app.core.audio.NoteReading
import com.afinu.app.core.util.MusicTheory

/**
 * Anúncio de "afinou" para o TalkBack.
 *
 * ## Por que isto é separado do resto da tela
 * Um afinador é, por natureza, um instrumento **visual e sonoro**. Para quem usa
 * TalkBack, o bip de confirmação existe — mas só 110 ms, e o TalkBack não o
 * anuncia. Sem um anúncio explícito, o usuário com leitor de tela não tem
 * nenhum sinal de que acertou, a não ser olhando o número de cents.
 *
 * ## Por que só na entrada da zona
 * Anunciar continuamente transformaria o afinador em uma máquina de falar. O
 * anúncio acontece uma vez, quando a nota entra na zona, que é o momento em que
 * há algo novo a dizer. O [trigger] é exatamente a transição, calculada no
 * ViewModel, não o estado "está afinado".
 *
 * ## Por que `LiveRegionMode.Polite` e não `Assertive`
 * Assertive interrompe o que o TalkBack estiver falando. O usuário costuma estar
 * tocando e ouvindo; interromper cada batida com um "afinou" ia atrapalhar mais
 * que ajudar. Polite espera a pausa natural.
 */
@Composable
fun InTuneAnnouncer(
    trigger: Boolean,
    reading: NoteReading?,
    a4Hz: Float,
    modifier: Modifier = Modifier,
) {
    val descricao = when {
        trigger && reading != null ->
            "${MusicTheory.spokenNameOf(reading.midi, a4Hz)}, afinado"

        reading != null ->
            "${MusicTheory.spokenNameOf(reading.midi, a4Hz)}, " +
                "${"%.0f".format(reading.cents)} cents"

        else -> "Nenhuma nota detectada"
    }

    // Nó invisível: existe apenas para o TalkBack ter o texto.
    // `key` força a recomposição do nó quando o texto muda, senão o leitor
    // anuncia a descrição antiga.
    androidx.compose.foundation.layout.Spacer(
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = descricao
        },
    )
}

/**
 * Rótulo estável + valor que muda, em um único nó.
 *
 * `stateDescription` é anunciado automaticamente quando o valor muda, sem que o
 * usuário precise navegar até o elemento. É o campo certo para valores que mudam
 * sozinhos — cents, BPM, casa do capo — enquanto `contentDescription` é para
 * rótulos fixos.
 *
 * Em um slider, sem isso o TalkBack anuncia "controle ajustável" e nada mais: o
 * usuário mexe no A4 ou no andamento às cegas.
 */
fun Modifier.labelAndState(label: String, value: String): Modifier =
    semantics {
        contentDescription = label
        androidx.compose.ui.semantics.stateDescription = value
    }

/**
 * Tamanho mínimo de alvo de toque.
 *
 * As diretrizes de acessibilidade pedem 48 dp. Metade disso é 24 dp, que é o
 * que o Material usa por padrão em controles pequenos — insuficiente para quem
 * tem tremor de mão, que é boa parte de quem usa este app com uma mão só,
 * segurando o instrumento na outra.
 */
object TouchTarget {
    const val MIN_DP = 48
    val MIN_SIZE = androidx.compose.ui.unit.Dp(MIN_DP.toFloat())
}