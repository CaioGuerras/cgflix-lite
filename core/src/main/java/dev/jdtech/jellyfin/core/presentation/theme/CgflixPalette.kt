package dev.jdtech.jellyfin.core.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * CGFLIX: todas as cores da marca num lugar só. As telas usam `MaterialTheme.colorScheme` (gerado
 * daqui em [ColorDark]) ou, para o que o Material não cobre (selos, número do "Em alta", esqueleto,
 * corações do Sobre), `MaterialTheme.cgflix` (app/phone). Nada de cor solta nas telas.
 *
 * Hoje só existe o tema "Isis" (escuro OLED, roxo). O tema claro verde ("Heitor") entra numa etapa
 * futura criando outra [CgflixPalette] e trocando o esquema, sem mexer nas telas.
 *
 * Contraste conferido sobre [background] (#07060a): [text] 16:1, [textMuted] 8:1, [accent] 5,3:1,
 * [lilac] 7,6:1 (todos ≥ 4,5:1).
 */
data class CgflixPalette(
    /** Fundo das telas (preto OLED). */
    val background: Color,
    /** Cartões, barra inferior, menus. */
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    /** Destaque principal (#a855f7): pílula da barra, chips ativos, links. */
    val accent: Color,
    /** Roxo forte (#9333ea): início do gradiente do botão principal. */
    val accentStrong: Color,
    /** Roxo escuro: contêineres. */
    val accentDeep: Color,
    /** Lilás (#c084fc): slogan, foco, textos de destaque. */
    val lilac: Color,
    /** Texto sobre o roxo. */
    val onAccent: Color,
    val text: Color,
    val textMuted: Color,
    val outline: Color,
    /** Placeholder dos esqueletos de carregamento (parado, sem brilho animado). */
    val skeleton: Color,
    /** Contorno do número grande do "Em alta". */
    val rankNumber: Color,
    /** Selo "Dublado". */
    val badgeDubbed: Color,
    /** Selo "Legendado". */
    val badgeSubtitled: Color,
    /** Texto dos selos. */
    val onBadge: Color,
    /** Selo "Pedido"/"Baixando" na busca. */
    val badgeRequest: Color,
    /** Corações da dedicatória do Sobre (roxo da Isis, verde do Heitor). */
    val heartIsis: Color,
    val heartHeitor: Color,
)

/** Tema escuro "Isis": fundo #07060a, destaque #a855f7. */
val CgflixIsis =
    CgflixPalette(
        background = Color(0xFF07060A),
        surface = Color(0xFF120E1A),
        surfaceHigh = Color(0xFF1B1526),
        surfaceHighest = Color(0xFF251D33),
        accent = Color(0xFFA855F7),
        accentStrong = Color(0xFF9333EA),
        accentDeep = Color(0xFF3B1F5C),
        lilac = Color(0xFFC084FC),
        onAccent = Color(0xFFFFFFFF),
        text = Color(0xFFE1E2E8),
        textMuted = Color(0xFFA9A3B5),
        outline = Color(0xFF8D9199),
        skeleton = Color(0xFF1B1526),
        rankNumber = Color(0xFFA855F7),
        badgeDubbed = Color(0xFF9333EA),
        badgeSubtitled = Color(0xFF3B1F5C),
        onBadge = Color(0xFFFFFFFF),
        badgeRequest = Color(0xFF251D33),
        heartIsis = Color(0xFFA855F7),
        heartHeitor = Color(0xFF22C55E),
    )
