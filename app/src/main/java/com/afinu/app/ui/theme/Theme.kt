package com.afinu.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = SurfaceLight,
    secondary = TunedGreenDark,
    background = DialBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = OffRedDark,
)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = SurfaceLight,
    secondary = TunedGreen,
    background = DialBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    error = OffRed,
)

@Composable
fun AfinadorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /**
     * Material You: a paleta segue o papel de parede do usuário (Android 12+).
     * Desligado, o app usa a identidade própria — o verde de "afinou" não pode
     * mudar de significado conforme o wallpaper.
     */
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AfinadorTypography,
        content = content,
    )
}