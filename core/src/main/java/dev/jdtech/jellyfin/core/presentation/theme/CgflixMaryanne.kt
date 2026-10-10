package dev.jdtech.jellyfin.core.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R

/**
 * CGFLIX (Modo Maryanne): tema do modo infantil. Claro, creme com rosa-morango e folhas verdes.
 * Não é escolha de tema: liga sozinho quando o app entra no Modo Maryanne e sai junto com ele.
 *
 * Contraste sobre o creme #fff7ee (teste `CgflixContrastTest`): [text] 14,2:1, [textMuted] 7,5:1,
 * [accent] 5,4:1, [lilac] 6,3:1, [outline] 3,9:1, [rankNumber] 3,9:1.
 */
val CgflixMaryanne =
    CgflixPalette(
        isLight = true,
        background = Color(0xFFFFF7EE),
        surface = Color(0xFFFCEBE4),
        surfaceHigh = Color(0xFFF9E1DA),
        surfaceHighest = Color(0xFFF5D7CF),
        accent = Color(0xFFC2244F),
        accentStrong = Color(0xFFA3163E),
        accentDeep = Color(0xFFFFDDE3),
        lilac = Color(0xFFB01F48),
        onAccent = Color(0xFFFFFFFF),
        text = Color(0xFF3B1E22),
        textMuted = Color(0xFF6B474B),
        outline = Color(0xFF9A7377),
        skeleton = Color(0xFFF9E1DA),
        rankNumber = Color(0xFFD9466C),
        badgeDubbed = Color(0xFFA3163E),
        // folha do morango
        badgeSubtitled = Color(0xFF2E7D32),
        onBadge = Color(0xFFFFFFFF),
        badgeRequest = Color(0xFF6B474B),
        heartIsis = Color(0xFF9333EA),
        heartHeitor = Color(0xFF1A9443),
        photoVeil = Color(0x26FFF7EE),
        cardElevation = 2.dp,
        banner = R.drawable.ic_banner_maryanne,
    )

/** Esquema do Material do Modo Maryanne (o app monta o `lightColorScheme` com estas cores). */
data object ColorMaryanne {
    val primary = CgflixMaryanne.accent
    val onPrimary = CgflixMaryanne.onAccent
    val primaryContainer = Color(0xFFFF8FA8)
    val onPrimaryContainer = Color(0xFF5C0A22)
    val secondary = Color(0xFF8E3B50)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = CgflixMaryanne.accentDeep
    val onSecondaryContainer = Color(0xFF7A1031)
    val tertiary = Color(0xFF2E7D32)
    val onTertiary = Color(0xFFFFFFFF)
    val tertiaryContainer = Color(0xFFB7E4B0)
    val onTertiaryContainer = Color(0xFF0F3D13)
    val error = Color(0xFFBA1A1A)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF93000A)
    val background = CgflixMaryanne.background
    val onBackground = CgflixMaryanne.text
    val surface = CgflixMaryanne.background
    val onSurface = CgflixMaryanne.text
    val surfaceVariant = Color(0xFFF6DEDB)
    val onSurfaceVariant = CgflixMaryanne.textMuted
    val outline = CgflixMaryanne.outline
    val outlineVariant = Color(0xFFE3C2C0)
    val scrim = Color(0xFF000000)
    val inverseSurface = Color(0xFF3F2A2C)
    val inverseOnSurface = Color(0xFFFFEDEA)
    val inversePrimary = Color(0xFFFFB1C1)
    val surfaceDim = Color(0xFFEBD4CD)
    val surfaceBright = CgflixMaryanne.background
    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainerLow = Color(0xFFFFF0E9)
    val surfaceContainer = CgflixMaryanne.surface
    val surfaceContainerHigh = CgflixMaryanne.surfaceHigh
    val surfaceContainerHighest = CgflixMaryanne.surfaceHighest
}
