package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.presentation.theme.ColorMaryanne

/*
 * CGFLIX (Modo Maryanne): esquema, cantos e fonte do modo infantil. Liga junto com o modo
 * (FindroidTheme(maryanne = true)); não aparece na escolha de tema.
 */

val maryanneScheme =
    lightColorScheme(
        primary = ColorMaryanne.primary,
        onPrimary = ColorMaryanne.onPrimary,
        primaryContainer = ColorMaryanne.primaryContainer,
        onPrimaryContainer = ColorMaryanne.onPrimaryContainer,
        secondary = ColorMaryanne.secondary,
        onSecondary = ColorMaryanne.onSecondary,
        secondaryContainer = ColorMaryanne.secondaryContainer,
        onSecondaryContainer = ColorMaryanne.onSecondaryContainer,
        tertiary = ColorMaryanne.tertiary,
        onTertiary = ColorMaryanne.onTertiary,
        tertiaryContainer = ColorMaryanne.tertiaryContainer,
        onTertiaryContainer = ColorMaryanne.onTertiaryContainer,
        error = ColorMaryanne.error,
        onError = ColorMaryanne.onError,
        errorContainer = ColorMaryanne.errorContainer,
        onErrorContainer = ColorMaryanne.onErrorContainer,
        background = ColorMaryanne.background,
        onBackground = ColorMaryanne.onBackground,
        surface = ColorMaryanne.surface,
        onSurface = ColorMaryanne.onSurface,
        surfaceVariant = ColorMaryanne.surfaceVariant,
        onSurfaceVariant = ColorMaryanne.onSurfaceVariant,
        outline = ColorMaryanne.outline,
        outlineVariant = ColorMaryanne.outlineVariant,
        scrim = ColorMaryanne.scrim,
        inverseSurface = ColorMaryanne.inverseSurface,
        inverseOnSurface = ColorMaryanne.inverseOnSurface,
        inversePrimary = ColorMaryanne.inversePrimary,
        surfaceDim = ColorMaryanne.surfaceDim,
        surfaceBright = ColorMaryanne.surfaceBright,
        surfaceContainerLowest = ColorMaryanne.surfaceContainerLowest,
        surfaceContainerLow = ColorMaryanne.surfaceContainerLow,
        surfaceContainer = ColorMaryanne.surfaceContainer,
        surfaceContainerHigh = ColorMaryanne.surfaceContainerHigh,
        surfaceContainerHighest = ColorMaryanne.surfaceContainerHighest,
    )

/** Cantos bem arredondados (o resto do app usa 10 dp nos menores). */
val maryanneShapes =
    Shapes(
        extraSmall = RoundedCornerShape(16.dp),
        small = RoundedCornerShape(18.dp),
        medium = RoundedCornerShape(22.dp),
        large = RoundedCornerShape(26.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

/** Fredoka (OFL, em res/font, recortada para o latim) só nos títulos; o corpo segue o padrão. */
private val fredoka =
    FontFamily(
        Font(
            R.font.cgflix_fredoka,
            FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            R.font.cgflix_fredoka,
            FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(500)),
        ),
        Font(
            R.font.cgflix_fredoka,
            FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600)),
        ),
        Font(
            R.font.cgflix_fredoka,
            FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
        ),
    )

val maryanneTypography =
    Typography().let { base ->
        base.copy(
            displayLarge =
                base.displayLarge.copy(fontFamily = fredoka, fontWeight = FontWeight.SemiBold),
            displayMedium =
                base.displayMedium.copy(fontFamily = fredoka, fontWeight = FontWeight.SemiBold),
            displaySmall =
                base.displaySmall.copy(fontFamily = fredoka, fontWeight = FontWeight.SemiBold),
            headlineLarge =
                base.headlineLarge.copy(fontFamily = fredoka, fontWeight = FontWeight.SemiBold),
            headlineMedium =
                base.headlineMedium.copy(fontFamily = fredoka, fontWeight = FontWeight.SemiBold),
            headlineSmall =
                base.headlineSmall.copy(fontFamily = fredoka, fontWeight = FontWeight.SemiBold),
            titleLarge =
                base.titleLarge.copy(fontFamily = fredoka, fontWeight = FontWeight.SemiBold),
            titleMedium =
                base.titleMedium.copy(fontFamily = fredoka, fontWeight = FontWeight.Medium),
            titleSmall = base.titleSmall.copy(fontFamily = fredoka, fontWeight = FontWeight.Medium),
        )
    }
