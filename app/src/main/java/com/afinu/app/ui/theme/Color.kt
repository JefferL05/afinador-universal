package com.afinu.app.ui.theme

import androidx.compose.ui.graphics.Color

// Verde = dentro da zona "no ponto". O app só pisca nesta cor quando a nota
// está de fato afinada, então ela precisa saltar à vista.
val TunedGreen = Color(0xFF22C55E)
val TunedGreenDark = Color(0xFF16A34A)

// Âmbar = perto, mas ainda fora. É o estado em que o usuário passa mais tempo.
val CloseAmber = Color(0xFFF59E0B)
val CloseAmberDark = Color(0xFFD97706)

// Vermelho = longe o bastante para nenhuma ação ser sugerida.
val OffRed = Color(0xFFEF4444)
val OffRedDark = Color(0xFFDC2626)

val BrandBlue = Color(0xFF3B82F6)
val BrandBlueDark = Color(0xFF2563EB)

// Fundo do medidor em modo escuro: quase preto, para o brilho da nota ser o
// único ponto alto da tela.
val DialBackgroundDark = Color(0xFF0B0F17)
val DialBackgroundLight = Color(0xFFF8FAFC)

val SurfaceDark = Color(0xFF111827)
val SurfaceLight = Color(0xFFFFFFFF)

// Variante de superfície para cartões e chips de ajuste.
val SurfaceVariantDark = Color(0xFF1F2937)
val SurfaceVariantLight = Color(0xFFEEF2F7)

val TextPrimaryDark = Color(0xFFF9FAFB)
val TextPrimaryLight = Color(0xFF111827)
val TextSecondaryDark = Color(0xFF9CA3AF)
val TextSecondaryLight = Color(0xFF6B7280)