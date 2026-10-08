package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// CGFLIX: botões com a cara da marca (só estilo; a disposição das telas não muda).
// Cores da paleta (MaterialTheme.cgflix), nada solto aqui.

/** Botão principal: pílula roxa com gradiente sutil (#9333ea → #a855f7) e texto semibold. */
@Composable
fun CgflixButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val palette = MaterialTheme.cgflix
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = CircleShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = palette.onAccent,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = palette.onAccent,
            ),
        border = if (focused) BorderStroke(2.dp, palette.lilac) else null,
        modifier =
            modifier
                .alpha(if (enabled) 1f else 0.38f)
                .background(
                    Brush.horizontalGradient(listOf(palette.accentStrong, palette.accent)),
                    CircleShape,
                ),
    ) {
        ProvideTextStyle(TextStyle(fontWeight = FontWeight.SemiBold)) { content() }
    }
}

/** Botão secundário: contorno roxo, texto lilás. */
@Composable
fun CgflixOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val palette = MaterialTheme.cgflix
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.lilac),
        border =
            BorderStroke(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) palette.lilac else palette.accent,
            ),
        modifier = modifier,
        content = content,
    )
}

@Preview
@Composable
private fun CgflixButtonsPreview() {
    FindroidTheme {
        CgflixButton(onClick = {}) { Text("Entrar") }
        CgflixOutlinedButton(onClick = {}) { Text("Saiba mais") }
    }
}
