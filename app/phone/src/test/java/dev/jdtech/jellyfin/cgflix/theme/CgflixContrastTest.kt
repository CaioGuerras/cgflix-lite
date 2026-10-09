package dev.jdtech.jellyfin.cgflix.theme

import androidx.compose.ui.graphics.Color
import dev.jdtech.jellyfin.core.presentation.theme.CgflixHeitor
import dev.jdtech.jellyfin.core.presentation.theme.CgflixIsis
import dev.jdtech.jellyfin.core.presentation.theme.CgflixPalette
import dev.jdtech.jellyfin.core.presentation.theme.ColorLight
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CGFLIX: contraste (WCAG 2.1) dos pares principais. Texto ≥ 4,5:1; bordas, ícones e o número
 * vazado do "Em alta" ≥ 3:1.
 */
class CgflixContrastTest {
    private fun channel(c: Float): Double =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    /** Cor no meio de um degradê linear (interpolado em sRGB, como o Android desenha). */
    private fun middle(a: Color, b: Color): Color =
        Color(
            red = (a.red + b.red) / 2,
            green = (a.green + b.green) / 2,
            blue = (a.blue + b.blue) / 2,
        )

    private fun assertContrast(name: String, fg: Color, bg: Color, minimum: Double) {
        val ratio = contrast(fg, bg)
        assertTrue("$name: %.2f:1 (mínimo %.1f:1)".format(ratio, minimum), ratio >= minimum)
    }

    private fun checkPalette(name: String, p: CgflixPalette) {
        // Texto
        assertContrast("$name texto/fundo", p.text, p.background, 4.5)
        assertContrast("$name texto/superfície", p.text, p.surface, 4.5)
        assertContrast("$name texto/superfície alta", p.text, p.surfaceHigh, 4.5)
        assertContrast("$name texto secundário/fundo", p.textMuted, p.background, 4.5)
        assertContrast("$name texto secundário/superfície", p.textMuted, p.surface, 4.5)
        assertContrast("$name destaque/fundo", p.accent, p.background, 4.5)
        assertContrast("$name lilás/fundo", p.lilac, p.background, 4.5)
        assertContrast("$name episódio em destaque", p.lilac, p.accentDeep, 4.5)
        // O rótulo do botão principal fica no meio do degradê accentStrong → accent
        assertContrast("$name botão (início do degradê)", p.onAccent, p.accentStrong, 4.5)
        assertContrast(
            "$name botão (meio do degradê)",
            p.onAccent,
            middle(p.accentStrong, p.accent),
            4.5,
        )
        assertContrast("$name selo Dublado", p.onBadge, p.badgeDubbed, 4.5)
        assertContrast("$name selo Legendado", p.onBadge, p.badgeSubtitled, 4.5)
        assertContrast("$name selo de pedido", p.onBadge, p.badgeRequest, 4.5)
        // Bordas e ícones
        assertContrast("$name contorno/fundo", p.outline, p.background, 3.0)
        assertContrast("$name número do Em alta", p.rankNumber, p.background, 3.0)
    }

    @Test fun `Isis passa nos pares principais`() = checkPalette("Isis", CgflixIsis)

    @Test fun `Heitor passa nos pares principais`() = checkPalette("Heitor", CgflixHeitor)

    @Test
    fun `esquema claro do Material (Heitor) passa nos pares principais`() {
        with(ColorLight) {
            assertContrast("onSurface/surface", onSurfaceLight, surfaceLight, 4.5)
            assertContrast("onSurfaceVariant/surface", onSurfaceVariantLight, surfaceLight, 4.5)
            assertContrast(
                "onSurfaceVariant/surfaceContainerHigh",
                onSurfaceVariantLight,
                surfaceContainerHighLight,
                4.5,
            )
            assertContrast("onSurface/cartão", onSurfaceLight, surfaceContainerLowestLight, 4.5)
            assertContrast("primary/surface", primaryLight, surfaceLight, 4.5)
            assertContrast("onPrimary/primary", onPrimaryLight, primaryLight, 4.5)
            assertContrast(
                "onPrimaryContainer/primaryContainer",
                onPrimaryContainerLight,
                primaryContainerLight,
                4.5,
            )
            assertContrast("ativo (primary)/pílula", primaryLight, secondaryContainerLight, 4.5)
            assertContrast(
                "onSecondaryContainer/secondaryContainer",
                onSecondaryContainerLight,
                secondaryContainerLight,
                4.5,
            )
            assertContrast("error/surface", errorLight, surfaceLight, 4.5)
            assertContrast("onError/error", onErrorLight, errorLight, 4.5)
            assertContrast("outline/surface", outlineLight, surfaceLight, 3.0)
            assertContrast("outline/cartão", outlineLight, surfaceContainerLowestLight, 3.0)
            assertContrast("tertiary/surface", tertiaryLight, surfaceLight, 4.5)
        }
    }

    @Test
    fun `paleta oficial do Heitor`() {
        assertEquals(Color(0xFFF4FCEE), ColorLight.surfaceLight)
        assertEquals(Color(0xFFFFFFFF), ColorLight.surfaceContainerLowestLight)
        assertEquals(Color(0xFFE8F0E3), ColorLight.surfaceContainerLight)
        assertEquals(Color(0xFFE2EBDE), ColorLight.surfaceContainerHighLight)
        assertEquals(Color(0xFF161D16), ColorLight.onSurfaceLight)
        assertEquals(Color(0xFF3D4A3C), ColorLight.onSurfaceVariantLight)
        assertEquals(Color(0xFF006E28), ColorLight.primaryLight)
        assertEquals(Color(0xFF34C759), ColorLight.primaryContainerLight)
        assertEquals(Color(0xFF004D1A), ColorLight.onPrimaryContainerLight)
        assertEquals(Color(0xFFB0EFB0), ColorLight.secondaryContainerLight)
        assertEquals(Color(0xFF6D7B6B), ColorLight.outlineLight)
        assertEquals(Color(0xFFBCCBB8), ColorLight.outlineVariantLight)
        assertEquals(Color(0xFF006495), ColorLight.tertiaryLight)
        assertEquals(Color(0xFFBA1A1A), ColorLight.errorLight)
    }
}
