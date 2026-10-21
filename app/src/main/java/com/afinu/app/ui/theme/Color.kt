package com.afinu.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Precision Studio Dark.
 *
 * ## O que é este palette
 * A identidade visual do app, desenhada para palco e estúdio: fundo quase
 * preto que não reflete luz, superfícies em degraus de cinza frio, e três
 * sinais de cor — verde, ciano e âmbar — que cada um significa exatamente
 * uma coisa.
 *
 * ## Por que a paleta é fixa
 * O app usava cor dinâmica (Material You), que troca o palette pelo papel de
 * parede do usuário. Aqui isso seria um erro: o verde significa "afinou" e o
 * ciano significa "esta é a nota", e nenhum dos dois pode virar cor de
 * wallpaper. Quem abre o app num palco escuro precisa que o verde continue
 * verde.
 *
 * ## Por que é só escuro
 * A interface é usada de olho fechado, com o instrumento no colo, sob luz
 * fraca. Um tema claro seria ofuscante. Não há versão clara por decisão de
 * projeto, não por falta de tempo.
 */

// --- Fundos e superfícies ---------------------------------------------------
// Degraus de elevação. Não há sombra projetada: a profundidade vem do tom.

/** Fundo do app. */
val StudioCanvas = Color(0xFF0F131C)

/** Painéis mais baixos que o cartão. */
val StudioSurfaceLowest = Color(0xFF0A0E17)

/** Faixas e barras. */
val StudioSurfaceLow = Color(0xFF181C25)

/** Base dos cartões. */
val StudioSurface = Color(0xFF1C2029)

/** Cartão em estado ativo, item selecionado. */
val StudioSurfaceHigh = Color(0xFF262A34)

/** Nível mais alto: menus e diálogos. */
val StudioSurfaceHighest = Color(0xFF31353F)

/** Variante de superfície, para chips e campos. */
val StudioSurfaceVariant = Color(0xFF31353F)

/** Texto principal. */
val StudioOnSurface = Color(0xFFDFE2EF)

/** Texto secundário e legendas. */
val StudioOnSurfaceVariant = Color(0xFFBACBB9)

/** Contorno de campo e borda de cartão. */
val StudioOutline = Color(0xFF859585)

/** Divisórias e bordas discretas. */
val StudioOutlineVariant = Color(0xFF3B4A3D)

// --- Sinal verde: no ponto ---------------------------------------------------

/** Verde de marca: o estado "afinou". */
val StudioGreen = Color(0xFF75FF9E)

/** Verde de fundo do estado "afinou". */
val StudioGreenContainer = Color(0xFF00E676)

/** Texto sobre verde de marca. */
val StudioOnGreen = Color(0xFF003918)

/** Texto sobre verde de fundo. */
val StudioOnGreenContainer = Color(0xFF00612E)

// --- Sinal ciano: a nota, os números ---------------------------------------

/** Ciano de marca: nota detectada e leitura em Hertz. */
val StudioCyan = Color(0xFFBDF4FF)

/** Ciano de fundo: nota detectada em destaque. */
val StudioCyanContainer = Color(0xFF00E3FD)

/** Texto sobre ciano de marca. */
val StudioOnCyan = Color(0xFF00363D)

/** Texto sobre ciano de fundo. */
val StudioOnCyanContainer = Color(0xFF00616D)

// --- Sinal âmbar: o desvio --------------------------------------------------

/** Âmbar de marca: desvios e avisos. */
val StudioAmber = Color(0xFFFFDEC5)

/** Âmbar de fundo: desvio e aviso. */
val StudioAmberContainer = Color(0xFFFFBA7D)

/** Texto sobre âmbar de marca. */
val StudioOnAmber = Color(0xFF4C2700)

/** Texto sobre âmbar de fundo. */
val StudioOnAmberContainer = Color(0xFF7F4500)

// --- Sinal vermelho: fora de faixa -----------------------------------------

/** Vermelho de marca: fora de faixa. */
val StudioError = Color(0xFFFFB4AB)

/** Texto sobre vermelho de marca. */
val StudioOnError = Color(0xFF690005)

/** Vermelho de fundo: erro. */
val StudioErrorContainer = Color(0xFF93000A)

/** Texto sobre vermelho de fundo. */
val StudioOnErrorContainer = Color(0xFFFFDAD6)

// --- Nomes de uso, os que o código usa -------------------------------------
// Estes são os papéis, não as cores: é por eles que o resto do app fala.

/**
 * Verde de "afinou".
 *
 * No app, o verde aparece quando a nota entra na zona de +/- 1,5 cent, e é
 * o único momento em que ele pisca. Precisa saltar à vista mesmo com o
 * instrumento no colo e o olho meio fechado.
 */
val TunedGreen = StudioGreenContainer

/**
 * Âmbar de "quase, mas ainda não".
 *
 * É o estado em que o usuário passa mais tempo: a nota está perto, mas
 * ainda precisa de ajuste.
 */
val CloseAmber = StudioAmberContainer

/**
 * Vermelho de "longe demais".
 *
 * Nenhuma ação é sugerida aqui: o usuário precisa corrigir antes de
 * qualquer coisa fazer sentido.
 */
val OffRed = Color(0xFFFF3D71)
