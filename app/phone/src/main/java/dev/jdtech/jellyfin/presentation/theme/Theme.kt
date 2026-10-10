package dev.jdtech.jellyfin.presentation.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.jdtech.jellyfin.core.presentation.theme.CgflixHeitor
import dev.jdtech.jellyfin.core.presentation.theme.CgflixIsis
import dev.jdtech.jellyfin.core.presentation.theme.CgflixMaryanne
import dev.jdtech.jellyfin.core.presentation.theme.Spacings

@Composable
fun FindroidTheme(
    darkTheme: Boolean? = isSystemInDarkTheme(), // CGFLIX: escuro = Isis, claro = Heitor
    dynamicColor: Boolean = true,
    // CGFLIX (Modo Maryanne): tema infantil claro; vence o escuro/claro e as cores dinâmicas
    maryanne: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = darkTheme ?: isSystemInDarkTheme()
    val colorScheme =
        when {
            maryanne -> maryanneScheme
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> darkScheme
            else -> lightScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = if (maryanne) maryanneShapes else shapes,
        typography = if (maryanne) maryanneTypography else MaterialTheme.typography,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.background),
            LocalSpacings provides Spacings,
            // CGFLIX: paleta da marca do tema ativo (Isis escuro, Heitor claro)
            LocalCgflixPalette provides
                when {
                    maryanne -> CgflixMaryanne
                    darkTheme -> CgflixIsis
                    else -> CgflixHeitor
                },
        ) {
            // CGFLIX: fundo do tema por baixo de todas as telas. Sem ele, telas sem Scaffold
            // (Login, Início, Você, fim de Detalhes) mostravam o fundo da janela, que segue o modo
            // noturno do sistema e pode ficar escuro com o Heitor (texto escuro sobre preto)
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) { content() }
        }
    }
}
