package dev.jdtech.jellyfin.core.presentation.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R

/**
 * CGFLIX: todas as cores da marca num lugar só. As telas usam `MaterialTheme.colorScheme` (gerado
 * daqui em [ColorDark]) ou, para o que o Material não cobre (selos, número do "Em alta", esqueleto,
 * corações do Sobre), `MaterialTheme.cgflix` (app/phone). Nada de cor solta nas telas.
 *
 * Dois temas: "Isis" (escuro OLED, roxo) e "Heitor" (claro, verde). As telas não sabem qual está
 * ativo: só leem a paleta. O esquema do Material de cada um fica em Color.kt (ColorDark/ColorLight).
 *
 * Contraste conferido (teste `CgflixContrastTest`): texto ≥ 4,5:1 e bordas/ícones ≥ 3:1 nos dois.
 */
data class CgflixPalette(
    /** `true` no tema claro (Heitor): ícones escuros nas barras do sistema. */
    val isLight: Boolean,
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
    /** Véu sobre as fotos de fundo (Detalhes), antes do degradê para o fundo. */
    val photoVeil: Color,
    /** Sombra dos cartões de pôster (0 no escuro: sombra não aparece no preto). */
    val cardElevation: Dp,
    /** Marca horizontal "C com play + CGFLIX" (vetor) deste tema. */
    @DrawableRes val banner: Int,
)

/** Tema escuro "Isis": fundo #07060a, destaque #a855f7. */
val CgflixIsis =
    CgflixPalette(
        isLight = false,
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
        photoVeil = Color(0x1A000000),
        cardElevation = 0.dp,
        banner = R.drawable.ic_banner,
    )

/**
 * Tema claro "Heitor": Material 3 com semente #34C759 (`SchemeContent`). Fundo #f4fcee (não branco
 * puro), cartões brancos, destaque #006e28. Contraste sobre o fundo: [text] 16,4:1, [textMuted]
 * 8,9:1, [accent]/[lilac] 6,1:1, [outline] 4,3:1, [rankNumber] 3,7:1.
 */
val CgflixHeitor =
    CgflixPalette(
        isLight = true,
        background = Color(0xFFF4FCEE),
        surface = Color(0xFFE8F0E3),
        surfaceHigh = Color(0xFFE2EBDE),
        surfaceHighest = Color(0xFFDDE5D8),
        accent = Color(0xFF006E28),
        accentStrong = Color(0xFF00531D),
        accentDeep = Color(0xFFB0EFB0),
        lilac = Color(0xFF006E28),
        onAccent = Color(0xFFFFFFFF),
        text = Color(0xFF161D16),
        textMuted = Color(0xFF3D4A3C),
        outline = Color(0xFF6D7B6B),
        skeleton = Color(0xFFE2EBDE),
        rankNumber = Color(0xFF1A9443),
        badgeDubbed = Color(0xFF006E28),
        badgeSubtitled = Color(0xFF006495),
        onBadge = Color(0xFFFFFFFF),
        badgeRequest = Color(0xFF3D4A3C),
        heartIsis = Color(0xFF9333EA),
        heartHeitor = Color(0xFF1A9443),
        photoVeil = Color(0x26F4FCEE),
        cardElevation = 2.dp,
        banner = R.drawable.ic_banner_heitor,
    )
