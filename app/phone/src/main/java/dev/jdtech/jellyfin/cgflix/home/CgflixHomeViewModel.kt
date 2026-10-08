package dev.jdtech.jellyfin.cgflix.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.cgflix.CgflixRepository
import dev.jdtech.jellyfin.cgflix.logic.CgflixCategory
import dev.jdtech.jellyfin.cgflix.logic.CgflixLibraryInfo
import dev.jdtech.jellyfin.cgflix.logic.CgflixRowSpec
import dev.jdtech.jellyfin.cgflix.logic.cgflixHomeRows
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** Situação de uma linha: carregando (esqueleto), pronta, ou escondida (vazia/erro). */
sealed interface CgflixRowState {
    data object Loading : CgflixRowState

    data class Ready(val items: List<FindroidItem>, val title: String? = null) : CgflixRowState

    data object Hidden : CgflixRowState
}

data class CgflixHomeState(
    val chips: List<Pair<CgflixCategory, CgflixLibraryInfo>> = emptyList(),
    val rows: List<CgflixRowSpec> = emptyList(),
    val rowStates: Map<String, CgflixRowState> = emptyMap(),
    /** Nada carregou (sem internet, servidor fora): mostra aviso com "Tentar de novo". */
    val failed: Boolean = false,
)

/**
 * CGFLIX (Etapa 1B): Início enxuta. Continuar assistindo → Em alta no Brasil → Filmes, Séries e
 * Animes recentes. Cada linha carrega sozinha (em paralelo, como na 1A) e aparece assim que chega;
 * enquanto isso, esqueleto parado. Volta para a Início sem recarregar se a carga tem < 60 s.
 */
@HiltViewModel
class CgflixHomeViewModel
@Inject
constructor(
    private val cgflix: CgflixRepository,
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(CgflixHomeState())
    val state = _state.asStateFlow()

    private var lastLoadedAt = 0L

    fun load(force: Boolean = false) {
        if (
            !force && lastLoadedAt != 0L && System.currentTimeMillis() - lastLoadedAt < REFRESH_MS
        ) {
            return
        }
        lastLoadedAt = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.Default) {
            val categories =
                try {
                    cgflix.categories(force)
                } catch (e: Exception) {
                    Timber.w(e, "CGFLIX: bibliotecas não carregaram")
                    null
                }
            // Sem rede numa recarga, mantém as categorias que já estavam na tela
            val known = categories ?: _state.value.chips.toMap()
            val rows = cgflixHomeRows(known)
            _state.update { current ->
                current.copy(
                    chips = known.toList(),
                    rows = rows,
                    // Recarga em segundo plano mantém o que já está na tela (sem esqueleto de novo)
                    rowStates =
                        rows.associate {
                            it.key to (current.rowStates[it.key] ?: CgflixRowState.Loading)
                        },
                    failed = false,
                )
            }
            var anyLoaded = false
            coroutineScope {
                for (row in rows) {
                    launch {
                        val result =
                            try {
                                loadRow(row, force)
                            } catch (e: Exception) {
                                Timber.w(e, "CGFLIX: linha ${row.key} falhou")
                                null
                            }
                        if (result != null) anyLoaded = true
                        val rowState =
                            if (result == null || result.items.isEmpty()) CgflixRowState.Hidden
                            else result
                        _state.update { it.copy(rowStates = it.rowStates + (row.key to rowState)) }
                    }
                }
            }
            if (categories == null && !anyLoaded) {
                lastLoadedAt = 0L
                _state.update { it.copy(failed = true) }
            }
        }
    }

    private suspend fun loadRow(row: CgflixRowSpec, force: Boolean): CgflixRowState.Ready? =
        when (row.kind) {
            CgflixRowSpec.Kind.CONTINUE ->
                if (appPreferences.getValue(appPreferences.homeContinueWatching)) {
                    CgflixRowState.Ready(repository.getResumeItems())
                } else {
                    null
                }
            CgflixRowSpec.Kind.TRENDING -> {
                val trending = cgflix.trending(force)
                val items =
                    trending
                        ?.homeIds
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { cgflix.itemsByIds(it, repository) }
                if (!items.isNullOrEmpty()) {
                    CgflixRowState.Ready(items, trending?.title)
                } else {
                    CgflixRowState.Ready(cgflix.trendingCollection(repository))
                }
            }
            CgflixRowSpec.Kind.RECENT ->
                if (appPreferences.getValue(appPreferences.homeLatest)) {
                    CgflixRowState.Ready(cgflix.recent(row.category!!, row.libraryId!!, repository))
                } else {
                    null
                }
        }

    private companion object {
        const val REFRESH_MS = 60_000L
    }
}
