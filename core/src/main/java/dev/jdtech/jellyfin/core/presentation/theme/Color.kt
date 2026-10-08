package dev.jdtech.jellyfin.core.presentation.theme

import androidx.compose.ui.graphics.Color

data object ColorLight {
    val primaryLight = Color(0xFF7E22CE)
    val onPrimaryLight = Color(0xFFFFFFFF)
    val primaryContainerLight = Color(0xFFF3E8FF)
    val onPrimaryContainerLight = Color(0xFF3B0764)
    val secondaryLight = Color(0xFF9333EA)
    val onSecondaryLight = Color(0xFFFFFFFF)
    val secondaryContainerLight = Color(0xFFEDE9FE)
    val onSecondaryContainerLight = Color(0xFF2E1065)
    val tertiaryLight = Color(0xFF6D5677)
    val onTertiaryLight = Color(0xFFFFFFFF)
    val tertiaryContainerLight = Color(0xFFF5D9FF)
    val onTertiaryContainerLight = Color(0xFF261430)
    val errorLight = Color(0xFFBA1A1A)
    val onErrorLight = Color(0xFFFFFFFF)
    val errorContainerLight = Color(0xFFFFDAD6)
    val onErrorContainerLight = Color(0xFF410002)
    val backgroundLight = Color(0xFFF8F9FF)
    val onBackgroundLight = Color(0xFF191C20)
    val surfaceLight = Color(0xFFF8F9FF)
    val onSurfaceLight = Color(0xFF191C20)
    val surfaceVariantLight = Color(0xFFDFE2EB)
    val onSurfaceVariantLight = Color(0xFF43474E)
    val outlineLight = Color(0xFF73777F)
    val outlineVariantLight = Color(0xFFC3C6CF)
    val scrimLight = Color(0xFF000000)
    val inverseSurfaceLight = Color(0xFF2E3035)
    val inverseOnSurfaceLight = Color(0xFFEFF0F7)
    val inversePrimaryLight = Color(0xFFC084FC)
    val surfaceDimLight = Color(0xFFD8DAE0)
    val surfaceBrightLight = Color(0xFFF8F9FF)
    val surfaceContainerLowestLight = Color(0xFFFFFFFF)
    val surfaceContainerLowLight = Color(0xFFF2F3FA)
    val surfaceContainerLight = Color(0xFFEDEDF4)
    val surfaceContainerHighLight = Color(0xFFE7E8EE)
    val surfaceContainerHighestLight = Color(0xFFE1E2E8)
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
