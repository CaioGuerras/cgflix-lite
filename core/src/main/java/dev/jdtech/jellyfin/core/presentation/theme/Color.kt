package dev.jdtech.jellyfin.core.presentation.theme

import androidx.compose.ui.graphics.Color

// CGFLIX: o tema claro é o "Heitor" (Material 3, semente #34C759, SchemeContent); a paleta da marca
// fica em CgflixPalette.kt e os papéis que não estão nela vêm do mesmo esquema gerado
data object ColorLight {
    val primaryLight = CgflixHeitor.accent
    val onPrimaryLight = CgflixHeitor.onAccent
    val primaryContainerLight = Color(0xFF34C759)
    val onPrimaryContainerLight = Color(0xFF004D1A)
    val secondaryLight = Color(0xFF316A38)
    val onSecondaryLight = Color(0xFFFFFFFF)
    val secondaryContainerLight = CgflixHeitor.accentDeep
    val onSecondaryContainerLight = Color(0xFF00531D)
    val tertiaryLight = Color(0xFF006495)
    val onTertiaryLight = Color(0xFFFFFFFF)
    val tertiaryContainerLight = Color(0xFF48B7FF)
    val onTertiaryContainerLight = Color(0xFF00466A)
    val errorLight = Color(0xFFBA1A1A)
    val onErrorLight = Color(0xFFFFFFFF)
    val errorContainerLight = Color(0xFFFFDAD6)
    val onErrorContainerLight = Color(0xFF93000A)
    val backgroundLight = CgflixHeitor.background
    val onBackgroundLight = CgflixHeitor.text
    val surfaceLight = CgflixHeitor.background
    val onSurfaceLight = CgflixHeitor.text
    val surfaceVariantLight = Color(0xFFD8E7D3)
    val onSurfaceVariantLight = CgflixHeitor.textMuted
    val outlineLight = CgflixHeitor.outline
    val outlineVariantLight = Color(0xFFBCCBB8)
    val scrimLight = Color(0xFF000000)
    val inverseSurfaceLight = Color(0xFF2B322A)
    val inverseOnSurfaceLight = Color(0xFFEBF3E6)
    val inversePrimaryLight = Color(0xFF53E16F)
    val surfaceDimLight = Color(0xFFD4DDD0)
    val surfaceBrightLight = CgflixHeitor.background
    val surfaceContainerLowestLight = Color(0xFFFFFFFF)
    val surfaceContainerLowLight = Color(0xFFEEF6E9)
    val surfaceContainerLight = CgflixHeitor.surface
    val surfaceContainerHighLight = CgflixHeitor.surfaceHigh
    val surfaceContainerHighestLight = CgflixHeitor.surfaceHighest
}

// CGFLIX: o tema escuro sai da paleta "Isis" (CgflixPalette.kt); mude as cores lá
data object ColorDark {
    val primaryDark = CgflixIsis.accent
    val onPrimaryDark = CgflixIsis.onAccent
    val primaryContainerDark = CgflixIsis.accentStrong
    val onPrimaryContainerDark = Color(0xFFFFFFFF)
    val secondaryDark = CgflixIsis.lilac
    val onSecondaryDark = Color(0xFF2E1065)
    val secondaryContainerDark = CgflixIsis.accentDeep
    val onSecondaryContainerDark = Color(0xFFF3E8FF)
    val tertiaryDark = Color(0xFFD9BDE3)
    val onTertiaryDark = Color(0xFF3C2947)
    val tertiaryContainerDark = Color(0xFF543F5E)
    val onTertiaryContainerDark = Color(0xFFF5D9FF)
    val errorDark = Color(0xFFFFB4AB)
    val onErrorDark = Color(0xFF690005)
    val errorContainerDark = Color(0xFF93000A)
    val onErrorContainerDark = Color(0xFFFFDAD6)
    val backgroundDark = CgflixIsis.background
    val onBackgroundDark = CgflixIsis.text
    val surfaceDark = CgflixIsis.background
    val onSurfaceDark = CgflixIsis.text
    val surfaceVariantDark = Color(0xFF2A2236)
    val onSurfaceVariantDark = Color(0xFFC3C6CF)
    val outlineDark = CgflixIsis.outline
    val outlineVariantDark = Color(0xFF2A2236)
    val scrimDark = Color(0xFF000000)
    val inverseSurfaceDark = Color(0xFFE1E2E8)
    val inverseOnSurfaceDark = Color(0xFF2E3035)
    val inversePrimaryDark = Color(0xFF7E22CE)
    val surfaceDimDark = Color(0xFF07060A)
    val surfaceBrightDark = Color(0xFF2F2640)
    val surfaceContainerLowestDark = CgflixIsis.background
    val surfaceContainerLowDark = Color(0xFF0E0B14)
    val surfaceContainerDark = CgflixIsis.surface
    val surfaceContainerHighDark = CgflixIsis.surfaceHigh
    val surfaceContainerHighestDark = CgflixIsis.surfaceHighest
}

val Yellow = Color(0xFFF2C94C)
