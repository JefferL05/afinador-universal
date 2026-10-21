package com.afinu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * A paleta do Precision Studio Dark.
 *
 * O Material 3 completa os papéis que o design não nomeia (inverseSurface,
 * scrim, surfaceTint) a partir dos que foram declarados, e é por isso que só
 * os papéis visíveis estão aqui: o resto sai de graça e sai certo.
 */
private val StudioColors = darkColorScheme(
    primary = StudioGreen,
    onPrimary = StudioOnGreen,
    primaryContainer = StudioGreenContainer,
    onPrimaryContainer = StudioOnGreenContainer,
    inversePrimary = StudioGreenContainer,

    secondary = StudioCyan,
    onSecondary = StudioOnCyan,
    secondaryContainer = StudioCyanContainer,
    onSecondaryContainer = StudioOnCyanContainer,

    tertiary = StudioAmber,
    onTertiary = StudioOnAmber,
    tertiaryContainer = StudioAmberContainer,
    onTertiaryContainer = StudioOnAmberContainer,

    error = StudioError,
    onError = StudioOnError,
    errorContainer = StudioErrorContainer,
    onErrorContainer = StudioOnErrorContainer,

    background = StudioCanvas,
    onBackground = StudioOnSurface,
    surface = StudioCanvas,
    onSurface = StudioOnSurface,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioOnSurfaceVariant,
    surfaceTint = StudioGreenContainer,
    inverseSurface = StudioOnSurface,
    inverseOnSurface = StudioSurfaceHighest,

    surfaceContainerLowest = StudioSurfaceLowest,
    surfaceContainerLow = StudioSurfaceLow,
    surfaceContainer = StudioSurface,
    surfaceContainerHigh = StudioSurfaceHigh,
    surfaceContainerHighest = StudioSurfaceHighest,
    surfaceBright = StudioSurfaceHighest,
    surfaceDim = StudioSurfaceLowest,

    outline = StudioOutline,
    outlineVariant = StudioOutlineVariant,
    scrim = StudioCanvas,
)

/**
 * As formas do design.
 *
 * O arredondamento base é de 8 px: é o raio de um pedal de guitarra e de um
 * chassi de rack. Controles arredondados de menos parecem_button de
 * computador; de mais, brinquedo.
 */
val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * O tema do app.
 *
 * ## Por que não tem modo claro
 * O app é usado com o instrumento no colo, em ambiente escuro, muitas vezes
 * com o olho focado demais no medidor para que a luz do celular incomode.
 * Um tema claro seria ofuscante. Não há versão clara por decisão de projeto.
 *
 * ## Por que não tem cor dinâmica
 * Com Material You, o palette vem do papel de parede. Aqui o verde significa
 * "afinou" e o ciano significa "esta é a nota" — nenhum dos dois pode
 * depender do wallpaper de quem abriu o app.
 *
 * O parâmetro [isSystemInDarkTheme] é mantido apenas para a assinatura
 * compatível com quem chama; o tema é escuro nos dois casos.
 */
@Composable
fun AfinadorTheme(
    @Suppress("UNUSED_PARAMETER") isSystemInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = StudioColors,
        typography = AfinadorTypography,
        shapes = StudioShapes,
        content = content,
    )
}
