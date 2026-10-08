package dev.jdtech.jellyfin.cgflix.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.cgflix.CgflixRepository
import dev.jdtech.jellyfin.cgflix.logic.CGFLIX_TRENDING_MIN_IN_CATEGORY
import dev.jdtech.jellyfin.cgflix.logic.CgflixCategory
import dev.jdtech.jellyfin.cgflix.logic.CgflixLibraryInfo
import dev.jdtech.jellyfin.cgflix.logic.CgflixRowSpec
import dev.jdtech.jellyfin.cgflix.logic.cgflixCategoryRows
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

data class CgflixCategoryState(
    val category: CgflixCategory? = null,
    val library: CgflixLibraryInfo? = null,
    val rows: List<CgflixRowSpec> = emptyList(),
    val rowStates: Map<String, CgflixRowState> = emptyMap(),
    /** Tudo da biblioteca, por nome, em páginas. */
    val all: Flow<PagingData<FindroidItem>>? = null,
    val missing: Boolean = false,
)

/**
 * CGFLIX (Etapa 1B): página de uma categoria (chip). Em alta da própria categoria (`porBiblioteca`
 * do emalta.json; some com menos de 3), Adicionados recentemente e a biblioteca inteira. Tudo
 * preso ao `ParentId` da biblioteca da categoria.
 */
@HiltViewModel
class CgflixCategoryViewModel
@Inject
constructor(private val cgflix: CgflixRepository, private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(CgflixCategoryState())
    val state = _state.asStateFlow()

    fun load(category: CgflixCategory) {
        if (_state.value.category == category) return
        _state.update { it.copy(category = category) }
        viewModelScope.launch(Dispatchers.Default) {
            val library =
                try {
                    cgflix.categories()[category]
                } catch (e: Exception) {
                    Timber.w(e, "CGFLIX: bibliotecas não carregaram")
                    null
                }
            if (library == null) {
                _state.update { it.copy(missing = true) }
                return@launch
            }
            val rows = cgflixCategoryRows(category, library)
            val type = if (category == CgflixCategory.FILMES) BaseItemKind.MOVIE else BaseItemKind.SERIES
            val all =
                repository
                    .getItemsPaging(
                        parentId = UUID.fromString(library.id),
                        includeTypes = listOf(type),
                        recursive = true,
                        sortBy = SortBy.NAME,
                    )
                    .cachedIn(viewModelScope)
            _state.update {
                it.copy(
                    library = library,
                    rows = rows,
                    rowStates = rows.associate { row -> row.key to CgflixRowState.Loading },
                    all = all,
                )
            }
            for (row in rows) {
                launch {
                    val result =
                        try {
                            loadRow(category, library, row)
                        } catch (e: Exception) {
                            Timber.w(e, "CGFLIX: linha ${row.key} falhou")
                            CgflixRowState.Hidden
                        }
                    _state.update { it.copy(rowStates = it.rowStates + (row.key to result)) }
                }
            }
        }
    }

    private suspend fun loadRow(
        category: CgflixCategory,
        library: CgflixLibraryInfo,
        row: CgflixRowSpec,
    ): CgflixRowState =
        when (row.kind) {
            CgflixRowSpec.Kind.TRENDING -> {
                val trending = cgflix.trending()
                val items =
                    if (trending != null) {
                        trending.idsFor(category)?.let { cgflix.itemsByIds(it, repository) }.orEmpty()
                    } else if (category == CgflixCategory.FILMES) {
                        // Sem o arquivo, só Filmes dá para separar (filme é sempre filme)
                        cgflix.trendingCollection(repository).filterIsInstance<FindroidMovie>()
                    } else {
                        emptyList()
                    }
                if (items.size < CGFLIX_TRENDING_MIN_IN_CATEGORY) CgflixRowState.Hidden
                else CgflixRowState.Ready(items)
            }
            CgflixRowSpec.Kind.RECENT ->
                cgflix.recent(category, library.id, repository).let {
                    if (it.isEmpty()) CgflixRowState.Hidden else CgflixRowState.Ready(it)
                }
            CgflixRowSpec.Kind.CONTINUE -> CgflixRowState.Hidden
        }
}
