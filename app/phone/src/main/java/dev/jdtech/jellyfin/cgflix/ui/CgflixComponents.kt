package dev.jdtech.jellyfin.cgflix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jdtech.jellyfin.cgflix.home.CgflixRowState
import dev.jdtech.jellyfin.cgflix.logic.CgflixLanguageBadges
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.film.components.ItemPoster
import dev.jdtech.jellyfin.presentation.theme.cgflix
import dev.jdtech.jellyfin.presentation.theme.spacings

// CGFLIX (Etapa 1B): peças leves das telas novas. Sem animação, sem imagem extra: o número do
// "Em alta" e o esqueleto são desenhados em Compose.

/** Slogan "Aperte o play": texto parado, em lilás (sem animação no Lite). */
@Composable
fun CgflixSlogan(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(CoreR.string.cgflix_slogan),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.cgflix.lilac,
        modifier = modifier,
    )
}

@Composable
fun CgflixSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.cgflix.text,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * Uma linha de pôsteres. [ranked] desenha o número grande vazado ao lado (Em alta); [horizontal]
 * usa a miniatura larga (Continuar assistindo, como no Findroid).
 */
@Composable
fun CgflixRow(
    title: String,
    state: CgflixRowState,
    contentPadding: PaddingValues,
    onItemClick: (FindroidItem) -> Unit,
    modifier: Modifier = Modifier,
    ranked: Boolean = false,
    horizontal: Boolean = false,
) {
    when (state) {
        CgflixRowState.Hidden -> Unit
        CgflixRowState.Loading -> CgflixSkeletonRow(contentPadding, horizontal, modifier)
        is CgflixRowState.Ready ->
            Column(modifier = modifier) {
                CgflixSectionTitle(
                    text = state.title ?: title,
                    modifier = Modifier.padding(contentPadding),
                )
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                LazyRow(
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                ) {
                    if (ranked) {
                        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                            CgflixRankedCard(rank = index + 1, item = item, onClick = onItemClick)
                        }
                    } else {
                        items(state.items, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                direction =
                                    if (horizontal) Direction.HORIZONTAL else Direction.VERTICAL,
                                onClick = onItemClick,
                            )
                        }
                    }
                }
            }
    }
}

/** Esqueleto parado (sem brilho animado): título e quatro cartões no tamanho certo. */
@Composable
fun CgflixSkeletonRow(
    contentPadding: PaddingValues,
    horizontal: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.cgflix.skeleton
    Column(modifier = modifier.semantics { contentDescription = "Carregando" }) {
        Box(
            Modifier.padding(contentPadding)
                .width(140.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(Modifier.height(MaterialTheme.spacings.small))
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        ) {
            repeat(4) {
                Box(
                    Modifier.width(if (horizontal) 260.dp else 150.dp)
                        .aspectRatio(if (horizontal) 1.77f else 0.66f)
                        .clip(MaterialTheme.shapes.small)
                        .background(color)
                )
            }
        }
    }
}

/** Pôster do "Em alta" com o número grande vazado ao lado (desenhado, sem imagem extra). */
@Composable
fun CgflixRankedCard(rank: Int, item: FindroidItem, onClick: (FindroidItem) -> Unit) {
    val palette = MaterialTheme.cgflix
    val elevation = palette.cardElevation
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier =
            Modifier.then(
                    if (elevation > 0.dp) Modifier else Modifier.clip(MaterialTheme.shapes.small)
                )
                .clickable { onClick(item) }
                .semantics(mergeDescendants = true) { contentDescription = "$rank. ${item.name}" },
    ) {
        Text(
            text = rank.toString(),
            style =
                TextStyle(
                    fontSize = 96.sp,
                    lineHeight = 96.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.rankNumber,
                    drawStyle = Stroke(width = 5f),
                ),
            maxLines = 1,
            modifier = Modifier.padding(end = 2.dp),
        )
        Column(Modifier.width(120.dp)) {
            ItemPoster(
                item = item,
                direction = Direction.VERTICAL,
                // sombra suave só no tema claro (Heitor); recorte arredondado nos dois
                modifier = Modifier.shadow(elevation, MaterialTheme.shapes.small, clip = true),
            )
            Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Chips Filmes · Séries · Animes no topo da Início. */
@Composable
fun CgflixChipsRow(
    labels: List<String>,
    onClick: (index: Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
) {
    val palette = MaterialTheme.cgflix
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        modifier = modifier,
    ) {
        itemsIndexed(labels) { index, label ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onClick(index) },
                label = { Text(label) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        labelColor = palette.text,
                        selectedContainerColor = palette.accent,
                        selectedLabelColor = palette.onAccent,
                    ),
            )
        }
    }
}

/** Selos "Dublado" e "Legendado" (nada aparece se não houver nenhum). */
@Composable
fun CgflixLanguageBadgesRow(badges: CgflixLanguageBadges?, modifier: Modifier = Modifier) {
    if (badges == null || badges.isEmpty) return
    val palette = MaterialTheme.cgflix
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        if (badges.dubbed) CgflixBadge("Dublado", palette.badgeDubbed)
        if (badges.subtitled) CgflixBadge("Legendado", palette.badgeSubtitled)
    }
}

@Composable
fun CgflixBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.cgflix.onBadge,
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color)
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Abertura (enquanto o app lê o servidor salvo): marca e slogan, parados. */
@Composable
fun CgflixOpening() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.cgflix.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(MaterialTheme.cgflix.banner),
                contentDescription = "CGFLIX Lite",
                modifier = Modifier.width(220.dp),
            )
            Spacer(Modifier.height(MaterialTheme.spacings.small))
            CgflixSlogan()
        }
    }
}
