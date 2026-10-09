package dev.jdtech.jellyfin.cgflix.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.cgflix.logic.CgflixRequestState
import dev.jdtech.jellyfin.cgflix.logic.CgflixRequestable
import dev.jdtech.jellyfin.cgflix.ui.CgflixBadge
import dev.jdtech.jellyfin.cgflix.ui.CgflixSectionTitle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.theme.CgflixButton
import dev.jdtech.jellyfin.presentation.theme.cgflix
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridCellsAdaptiveWithMinColumns
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

/** CGFLIX (Etapa 1B): aba Buscar. */
@Composable
fun CgflixSearchScreen(
    onItemClick: (FindroidItem) -> Unit,
    viewModel: CgflixSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(true) { viewModel.messageFlow.collect { snackbar.showSnackbar(it) } }
    CgflixSearchLayout(
        state = state,
        snackbar = snackbar,
        onQueryChange = viewModel::onQueryChange,
        onItemClick = onItemClick,
        onRequest = viewModel::request,
    )
}

@Composable
fun CgflixSearchLayout(
    state: CgflixSearchState,
    snackbar: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onItemClick: (FindroidItem) -> Unit,
    onRequest: (CgflixRequestable) -> Unit,
) {
    val safePadding = rememberSafePadding(handleStartInsets = false)
    val focusManager = LocalFocusManager.current
    val palette = MaterialTheme.cgflix

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCellsAdaptiveWithMinColumns(minSize = 160.dp, minColumns = 2),
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = safePadding.start + MaterialTheme.spacings.default,
                    end = safePadding.end + MaterialTheme.spacings.default,
                    top = safePadding.top + MaterialTheme.spacings.small,
                    bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacings.default,
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        ) {
            item(key = "campo", span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Filmes, séries e animes") },
                    singleLine = true,
                    shape = CircleShape,
                    leadingIcon = {
                        Icon(
                            painterResource(CoreR.drawable.ic_cgflix_search),
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    painterResource(CoreR.drawable.ic_x),
                                    contentDescription = "Limpar busca",
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )
            }

            val results = state.results
            when {
                state.query.isBlank() ->
                    item(key = "dica", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Procure pelo nome. Se ainda não tivermos, dá para pedir aqui mesmo.",
                            color = palette.textMuted,
                        )
                    }
                state.loading || results == null ->
                    item(key = "buscando", span = { GridItemSpan(maxLineSpan) }) {
                        Text("Buscando…", color = palette.textMuted)
                    }
                results.isEmpty() ->
                    item(key = "nada", span = { GridItemSpan(maxLineSpan) }) {
                        Text("Nada com esse nome no CGFLIX.", color = palette.textMuted)
                    }
                else -> {
                    item(key = "nossos", span = { GridItemSpan(maxLineSpan) }) {
                        CgflixSectionTitle("No CGFLIX")
                    }
                    items(results, key = { it.id }) { item ->
                        ItemCard(item = item, direction = Direction.VERTICAL, onClick = onItemClick)
                    }
                }
            }

            when (val section = state.requests) {
                CgflixRequestsSection.Idle -> Unit
                CgflixRequestsSection.Loading ->
                    item(key = "pedir-carregando", span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            CgflixSectionTitle("Disponível para pedir")
                            Spacer(Modifier.height(MaterialTheme.spacings.small))
                            Text("Procurando…", color = palette.textMuted)
                        }
                    }
                CgflixRequestsSection.Unavailable ->
                    item(key = "pedir-fora", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Pedidos indisponíveis agora",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textMuted,
                        )
                    }
                is CgflixRequestsSection.Results ->
                    if (section.items.isNotEmpty()) {
                        item(key = "pedir", span = { GridItemSpan(maxLineSpan) }) {
                            CgflixSectionTitle("Disponível para pedir")
                        }
                        items(
                            section.items,
                            key = { "seerr-${if (it.isMovie) "f" else "s"}-${it.tmdbId}" },
                            span = { GridItemSpan(maxLineSpan) },
                        ) { item ->
                            CgflixRequestableRow(
                                item = item,
                                busy = item.tmdbId in state.requesting,
                                onRequest = { onRequest(item) },
                            )
                        }
                    }
            }
        }
    }
}

/** Um título do Seerr: pôster (pelo proxy do Seerr), nome, ano e "Pedir" ou o selo. */
@Composable
fun CgflixRequestableRow(item: CgflixRequestable, busy: Boolean, onRequest: () -> Unit) {
    val palette = MaterialTheme.cgflix
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        CgflixSeerrPoster(item.posterUrl, item.title)
        Spacer(Modifier.width(MaterialTheme.spacings.medium))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(if (item.isMovie) "Filme" else "Série", item.year?.toString())
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted,
            )
        }
        Spacer(Modifier.width(MaterialTheme.spacings.small))
        when (item.state) {
            CgflixRequestState.REQUESTABLE ->
                CgflixButton(onClick = onRequest, enabled = !busy) {
                    Text(if (busy) "Pedindo…" else "Pedir")
                }
            CgflixRequestState.REQUESTED -> CgflixBadge("Pedido", palette.badgeRequest)
            CgflixRequestState.DOWNLOADING -> CgflixBadge("Baixando", palette.badgeRequest)
        }
    }
}

@Composable
fun CgflixSeerrPoster(url: String?, title: String) {
    val modifier =
        Modifier.size(width = 60.dp, height = 90.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.cgflix.skeleton)
            .semantics { contentDescription = "Capa de $title" }
    if (url == null) {
        Box(modifier)
    } else {
        AsyncImage(model = url, contentDescription = null, modifier = modifier)
    }
}
