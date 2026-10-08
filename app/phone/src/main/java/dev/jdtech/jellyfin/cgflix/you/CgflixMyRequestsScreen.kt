package dev.jdtech.jellyfin.cgflix.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.cgflix.CgflixRepository
import dev.jdtech.jellyfin.cgflix.logic.CgflixMyRequest
import dev.jdtech.jellyfin.cgflix.search.CgflixSeerrPoster
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.CgflixButton
import dev.jdtech.jellyfin.presentation.theme.cgflix
import dev.jdtech.jellyfin.presentation.theme.spacings
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface CgflixMyRequestsState {
    data object Loading : CgflixMyRequestsState

    data class Ready(val items: List<CgflixMyRequest>) : CgflixMyRequestsState

    data object Unavailable : CgflixMyRequestsState
}

@HiltViewModel
class CgflixMyRequestsViewModel @Inject constructor(private val cgflix: CgflixRepository) :
    ViewModel() {
    private val _state = MutableStateFlow<CgflixMyRequestsState>(CgflixMyRequestsState.Loading)
    val state = _state.asStateFlow()

    fun load() {
        _state.value = CgflixMyRequestsState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            _state.value =
                try {
                    CgflixMyRequestsState.Ready(cgflix.seerr().myRequests())
                } catch (e: Exception) {
                    Timber.i("CGFLIX: Meus pedidos indisponíveis (${e.message})")
                    CgflixMyRequestsState.Unavailable
                }
        }
    }
}

/** CGFLIX (Etapa 1B): "Meus pedidos", lista nativa simples (título e situação). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CgflixMyRequestsScreen(
    navigateBack: () -> Unit,
    viewModel: CgflixMyRequestsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(true) { viewModel.load() }
    val palette = MaterialTheme.cgflix

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus pedidos") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(painterResource(CoreR.drawable.ic_arrow_left), contentDescription = "Voltar")
                    }
                },
            )
        }
    ) { innerPadding ->
        when (val s = state) {
            CgflixMyRequestsState.Loading ->
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Carregando…", color = palette.textMuted)
                }
            CgflixMyRequestsState.Unavailable ->
                Column(
                    Modifier.fillMaxSize().padding(innerPadding).padding(MaterialTheme.spacings.default),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Pedidos indisponíveis agora.", color = palette.textMuted, textAlign = TextAlign.Center)
                    Spacer(Modifier.padding(top = MaterialTheme.spacings.medium))
                    CgflixButton(onClick = viewModel::load) { Text("Tentar de novo") }
                }
            is CgflixMyRequestsState.Ready ->
                if (s.items.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(innerPadding).padding(MaterialTheme.spacings.default),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Você ainda não pediu nada. Peça pela Busca.",
                            color = palette.textMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding =
                            PaddingValues(
                                start = MaterialTheme.spacings.default,
                                end = MaterialTheme.spacings.default,
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacings.default,
                            ),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                    ) {
                        items(s.items, key = { it.id }) { request ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                CgflixSeerrPoster(request.posterUrl, request.title)
                                Spacer(Modifier.width(MaterialTheme.spacings.medium))
                                Column(Modifier.weight(1f)) {
                                    // Dado do Seerr só como texto
                                    Text(
                                        request.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        listOfNotNull(
                                                if (request.isMovie) "Filme" else "Série",
                                                request.year?.toString(),
                                            )
                                            .joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textMuted,
                                    )
                                    Text(
                                        request.status.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.lilac,
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }
}
