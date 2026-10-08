package dev.jdtech.jellyfin.presentation.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dev.jdtech.jellyfin.core.presentation.theme.CgflixIsis
import dev.jdtech.jellyfin.core.presentation.theme.CgflixPalette

// CGFLIX: paleta da marca disponível nas telas como `MaterialTheme.cgflix` (ver CgflixPalette.kt).
// Trocar o tema (ex.: o claro "Heitor", numa etapa futura) é trocar o valor deste Local.
val LocalCgflixPalette = staticCompositionLocalOf { CgflixIsis }

val MaterialTheme.cgflix: CgflixPalette
    @Composable @ReadOnlyComposable get() = LocalCgflixPalette.current

/**
 * CGFLIX: `true` quando a pessoa desligou as animações do sistema ("Remover animações" /
 * escala de duração do animador = 0). As telas do CGFLIX trocam transições por cortes secos.
 */
@Composable
fun rememberCgflixReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
