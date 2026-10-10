package dev.jdtech.jellyfin.cgflix.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.cgflix.logic.CgflixCategory
import dev.jdtech.jellyfin.cgflix.logic.CgflixRowSpec
import dev.jdtech.jellyfin.cgflix.ui.CgflixChipsRow
import dev.jdtech.jellyfin.cgflix.ui.CgflixRow
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.CgflixButton
import dev.jdtech.jellyfin.presentation.theme.cgflix
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

/** CGFLIX (Etapa 1B): Início do Lite. Substitui a Início do Findroid (que continua no código). */
@Composable
fun CgflixHomeScreen(
    onCategoryClick: (CgflixCategory) -> Unit,
    onItemClick: (FindroidItem) -> Unit,
    viewModel: CgflixHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(true) { viewModel.load() }
    LaunchedEffect(state.pronta) { if (state.pronta) viewModel.avisarPronta() }
    CgflixHomeLayout(
        state = state,
        onRefresh = { viewModel.load(force = true) },
        onCategoryClick = onCategoryClick,
        onItemClick = onItemClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CgflixHomeLayout(
    state: CgflixHomeState,
    onRefresh: () -> Unit,
    onCategoryClick: (CgflixCategory) -> Unit,
    onItemClick: (FindroidItem) -> Unit,
) {
    val safePadding = rememberSafePadding(handleStartInsets = false)
    val horizontal =
        PaddingValues(
            start = safePadding.start + MaterialTheme.spacings.default,
            end = safePadding.end + MaterialTheme.spacings.default,
        )
    // A posição da rolagem fica guardada enquanto a Início está na pilha (voltar mantém a rolagem)
    val listState = rememberLazyListState()

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = safePadding.top + MaterialTheme.spacings.small,
                    bottom = safePadding.bottom + MaterialTheme.spacings.default,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            item(key = "topo") {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal).height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(MaterialTheme.cgflix.banner),
                            contentDescription = "CGFLIX Lite",
                            modifier = Modifier.height(28.dp),
                        )
                    }
                    if (state.chips.isNotEmpty()) {
                        CgflixChipsRow(
                            labels = state.chips.map { it.first.label },
                            onClick = { index -> onCategoryClick(state.chips[index].first) },
                            contentPadding = horizontal,
                        )
                    }
                }
            }
            if (state.failed) {
                item(key = "falhou") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal).padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Não deu para falar com o servidor. Confira a internet.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.cgflix.textMuted,
                        )
                        Spacer(Modifier.height(MaterialTheme.spacings.medium))
                        CgflixButton(onClick = onRefresh) { Text("Tentar de novo") }
                    }
                }
            }
            items(state.rows, key = { it.key }) { row ->
                CgflixRow(
                    title = row.title,
                    state = state.rowStates[row.key] ?: CgflixRowState.Loading,
                    contentPadding = horizontal,
                    onItemClick = onItemClick,
                    ranked = row.kind == CgflixRowSpec.Kind.TRENDING,
                    horizontal = row.kind == CgflixRowSpec.Kind.CONTINUE,
                )
            }
        }
    }
}
