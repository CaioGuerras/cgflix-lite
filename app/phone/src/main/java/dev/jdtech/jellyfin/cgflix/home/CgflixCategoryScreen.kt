package dev.jdtech.jellyfin.cgflix.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.jdtech.jellyfin.cgflix.logic.CgflixCategory
import dev.jdtech.jellyfin.cgflix.logic.CgflixRowSpec
import dev.jdtech.jellyfin.cgflix.ui.CgflixRow
import dev.jdtech.jellyfin.cgflix.ui.CgflixSectionTitle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.theme.cgflix
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridCellsAdaptiveWithMinColumns
import kotlinx.coroutines.flow.emptyFlow

/** CGFLIX (Etapa 1B): página de uma categoria (Filmes, Séries ou Animes). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CgflixCategoryScreen(
    category: CgflixCategory,
    onItemClick: (FindroidItem) -> Unit,
    navigateBack: () -> Unit,
    viewModel: CgflixCategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(category) { viewModel.load(category) }
    val items = (state.all ?: emptyFlow()).collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.label) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = "Voltar",
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        if (state.missing) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Essa categoria não existe neste servidor.", color = MaterialTheme.cgflix.textMuted)
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCellsAdaptiveWithMinColumns(minSize = 160.dp, minColumns = 2),
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = MaterialTheme.spacings.default,
                    end = MaterialTheme.spacings.default,
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacings.default,
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        ) {
            for (row in state.rows) {
                item(key = row.key, span = { GridItemSpan(maxLineSpan) }) {
                    // As linhas encostam na borda da tela (a grade tem margem própria)
                    CgflixRow(
                        title = row.title,
                        state = state.rowStates[row.key] ?: CgflixRowState.Loading,
                        contentPadding = PaddingValues(0.dp),
                        onItemClick = onItemClick,
                        ranked = row.kind == CgflixRowSpec.Kind.TRENDING,
                    )
                }
            }
            if (items.itemCount > 0) {
                item(key = "todos", span = { GridItemSpan(maxLineSpan) }) {
                    Column { CgflixSectionTitle("Todos de ${category.label}") }
                }
            }
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                items[index]?.let { item ->
                    ItemCard(
                        item = item,
                        direction = Direction.VERTICAL,
                        onClick = onItemClick,
                    )
                }
            }
        }
    }
}
