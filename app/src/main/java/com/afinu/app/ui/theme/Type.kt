package com.afinu.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.afinu.app.R

/**
 * Inter: notas, títulos e navegação.
 *
 * Inter é a voz do app. Tem desenho neutro, terminal aberto e x-height alto.
 * O que mais importa aqui: os algarismos não se confundem entre si, o que
 * importa em uma tela onde "0" e "8" precisam ser lidos de relance.
 */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

/**
 * JetBrains Mono: tudo que é número.
 *
 * A razão de um monoespaçado para a leitura numérica é o **alinhamento
 * vertical**. Durante uma afinação o número muda a cada quadro, e num
 * fontes proporcionais o texto "sobe e desce" a cada troca: o "1" fica
 * mais estreito que o "0", a vírgula desloca, e o olho perde a posição.
 * Com monoespaçado o campo não se move, e a leitura fica estável.
 */
val MonoFamily = FontFamily(
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
    Font(R.font.jetbrainsmono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
)

/**
 * A nota musical detectada, no tamanho grande.
 *
 * ExtraBold em 56 sp: a nota é lida de longe, com o instrumento no colo, e
 * precisa ser a coisa mais presente da tela.
 */
val DisplayNote = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 56.sp,
    lineHeight = 56.sp,
    letterSpacing = (-0.03).em,
)

/**
 * Leitura numérica grande: Hertz, BPM.
 */
val DataMonoLarge = TextStyle(
    fontFamily = MonoFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.02).em,
)

/**
 * Leitura numérica média: desvio em cents, frequência de corda.
 */
val DataMonoMedium = TextStyle(
    fontFamily = MonoFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.02.em,
)

/**
 * Rótulo de painel de equipamento.
 *
 * Maiúscula, espaçada, monoespaçada: é a etiqueta de um botão de mesa de
 * som, e o espaçamento largo é o que separa "rótulo" de "frase".
 */
val LabelCaps = TextStyle(
    fontFamily = MonoFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.08.em,
)

/**
 * A escala do Material 3, reescrita com as duas famílias.
 *
 * Os nomes de slot são mantidos porque é o que o Compose espera, mas o
 * conteúdo é o do design novo: Inter nos slots de texto, e um `label` bem
 * mais estreito, que é o `label-caps` do design.
 */
val AfinadorTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.03).em,
    ),
    displayMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.03).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = LabelCaps,
    labelMedium = LabelCaps,
    labelSmall = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.08.em,
    ),
)
