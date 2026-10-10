package dev.jdtech.jellyfin.cgflix.maryanne

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.cgflix
import dev.jdtech.jellyfin.presentation.theme.rememberCgflixReduceMotion
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlinx.coroutines.delay

// CGFLIX (Modo Maryanne): peças visuais do modo infantil. Morango no lugar do "vazio" e do
// "carregando"; cobertura cheia enquanto a sessão troca de usuário.

/** `true` quando o app está no Modo Maryanne (as telas trocam textos e escondem itens). */
val LocalCgflixMaryanne = staticCompositionLocalOf { false }

/** A apresentação (clara) está na tela: a MainActivity deixa os ícones das barras escuros. */
val maryanneIntroAberta = mutableStateOf(false)

/** Tempo mínimo da cobertura na tela (não pisca) e o máximo (servidor lento não prende o app). */
private const val COBERTURA_MIN_MS = 1_500L
private const val COBERTURA_MAX_MS = 8_000L

/** Morango que pula devagar (parado quando a pessoa desligou as animações do sistema). */
@Composable
fun CgflixMorangoPulando(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val reduceMotion = rememberCgflixReduceMotion()
    val pulo =
        if (reduceMotion) {
            0f
        } else {
            val transicao = rememberInfiniteTransition(label = "morango")
            val valor by
                transicao.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
                    label = "pulo",
                )
            valor
        }
    Icon(
        painter = painterResource(CoreR.drawable.ic_cgflix_morango),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier =
            modifier.size(size).graphicsLayer {
                translationY = -pulo * size.toPx() * 0.28f
                // Achata um pouco ao tocar o chão
                val achatado = 1f - (1f - pulo) * 0.08f
                scaleY = achatado
                scaleX = 2f - achatado
            },
    )
}

/** Três morangos em fila, para o "carregando" das telas no modo. */
@Composable
fun CgflixMaryanneCarregando(modifier: Modifier = Modifier, texto: String = "Carregando…") {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp).testTag("maryanne_carregando"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { CgflixMorangoPulando(size = 32.dp) }
        }
        Spacer(Modifier.height(MaterialTheme.spacings.small))
        Text(texto, color = MaterialTheme.cgflix.textMuted, textAlign = TextAlign.Center)
    }
}

/** Morango parado com um texto, para as telas vazias no modo. */
@Composable
fun CgflixMaryanneVazio(texto: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp).testTag("maryanne_vazio"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(CoreR.drawable.ic_cgflix_morango),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(MaterialTheme.spacings.small))
        Text(texto, color = MaterialTheme.cgflix.textMuted, textAlign = TextAlign.Center)
    }
}

/**
 * Cobertura "Modo Maryanne ativado" sobre o app inteiro. Fica até a home nova avisar que carregou,
 * no mínimo [COBERTURA_MIN_MS]; some sozinha aos [COBERTURA_MAX_MS] se a home não avisar.
 */
@Composable
fun CgflixMaryanneCoberturaOverlay(cobertura: CgflixMaryanneCobertura?, onFechar: () -> Unit) {
    // Guarda a última para o texto não sumir durante o esmaecer
    val ultima = remember { arrayOfNulls<CgflixMaryanneCobertura>(1) }
    if (cobertura != null) ultima[0] = cobertura

    if (cobertura != null) {
        LaunchedEffect(cobertura.desde, cobertura.homePronta) {
            val passou = System.currentTimeMillis() - cobertura.desde
            if (cobertura.homePronta) {
                delay((COBERTURA_MIN_MS - passou).coerceAtLeast(0))
            } else {
                delay((COBERTURA_MAX_MS - passou).coerceAtLeast(0))
            }
            onFechar()
        }
    }

    AnimatedVisibility(visible = cobertura != null, enter = fadeIn(), exit = fadeOut()) {
        val atual = ultima[0] ?: return@AnimatedVisibility
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(MaterialTheme.cgflix.background)
                    // Segura os toques enquanto a sessão troca
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .testTag("maryanne_cobertura"),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(CoreR.drawable.ic_banner_maryanne),
                    contentDescription = "CGFLIX",
                    modifier = Modifier.width(220.dp),
                )
                Spacer(Modifier.height(MaterialTheme.spacings.large))
                CgflixMorangoPulando(size = 72.dp)
                Spacer(Modifier.height(MaterialTheme.spacings.medium))
                Text(
                    text = atual.texto,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.cgflix.accent,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.padding(horizontal = 24.dp).semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = atual.texto
                        },
                )
            }
        }
    }
}
