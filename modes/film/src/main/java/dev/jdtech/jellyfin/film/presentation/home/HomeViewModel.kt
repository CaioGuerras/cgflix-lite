package dev.jdtech.jellyfin.film.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.toView
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    val repository: JellyfinRepository,
    val appPreferences: AppPreferences,
    val database: ServerDatabaseDao,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val uuidSuggestions = UUID.fromString("31e47044-9b79-4bb0-99d0-0e477ed65420")
    private val uuidContinueWatching =
        UUID(4937169328197226115, -4704919157662094443) // 44845958-8326-4e83-beb4-c4f42e9eeb95
    private val uuidNextUp =
        UUID(1783371395749072194, -6164625418200444295) // 18bfced5-f237-4d42-aa72-d9d7fed19279

    private val uiTextContinueWatching = UiText.StringResource(FilmR.string.continue_watching)
    private val uiTextNextUp = UiText.StringResource(FilmR.string.next_up)

    // CGFLIX: instante da última carga bem-sucedida (para não recarregar a Início ao voltar a ela)
    private var lastLoadedAt = 0L
    private var hasLoaded = false

    /**
     * CGFLIX: [force] vem do "puxar para atualizar" e do botão de tentar de novo. Sem [force], não
     * recarrega se a última carga tem menos de [REFRESH_MIN_INTERVAL_MS]. O indicador de
     * carregamento só aparece na 1ª carga ou ao forçar; nas demais, a tela mostra os dados que já
     * tem e atualiza em segundo plano.
     */
    fun loadData(force: Boolean = false) {
        if (
            !force &&
                hasLoaded &&
                System.currentTimeMillis() - lastLoadedAt < REFRESH_MIN_INTERVAL_MS
        ) {
            return
        }
        Timber.i("Loading data")
        viewModelScope.launch(Dispatchers.Default) {
            val showIndicator = force || !hasLoaded
            _state.update { it.copy(isLoading = showIndicator, error = null) }
            try {
                appPreferences.getValue(appPreferences.currentServer)?.let { serverId ->
                    loadServerName(serverId)
                }

                // CGFLIX: as quatro seções são independentes, então carregam em paralelo
                coroutineScope {
                    launch { loadSuggestions() }
                    launch { loadResumeItems() }
                    launch { loadNextUpItems() }
                    launch { loadViews() }
                }
                hasLoaded = true
                lastLoadedAt = System.currentTimeMillis()
            } catch (e: Exception) {
                _state.update { it.copy(error = e) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadServerName(serverId: String) {
        val server = database.getServer(serverId)
        if (server != null) {
            _state.update { it.copy(server = server) }
        }
    }

    private suspend fun loadSuggestions() {
        Timber.i("Loading suggestions")
        if (!appPreferences.getValue(appPreferences.homeSuggestions)) {
            _state.update { it.copy(suggestionsSection = null) }
            return
        }

        val items = repository.getSuggestions()

        val section =
            if (items.isEmpty()) {
                null
            } else {
                HomeItem.Suggestions(id = uuidSuggestions, items = items)
            }

        _state.update { it.copy(suggestionsSection = section) }
    }

    private suspend fun loadResumeItems() {
        Timber.i("Loading resume items")
        if (!appPreferences.getValue(appPreferences.homeContinueWatching)) {
            _state.update { it.copy(resumeSection = null) }
            return
        }

        val resumeItems = repository.getResumeItems()

        val section =
            if (resumeItems.isEmpty()) {
                null
            } else {
                HomeItem.Section(
                    HomeSection(uuidContinueWatching, uiTextContinueWatching, resumeItems)
                )
            }

        _state.update { it.copy(resumeSection = section) }
    }

    private suspend fun loadNextUpItems() {
        Timber.i("Loading next up items")
        if (!appPreferences.getValue(appPreferences.homeNextUp)) {
            _state.update { it.copy(nextUpSection = null) }
            return
        }

        val nextUpItems = repository.getNextUp()

        val section =
            if (nextUpItems.isEmpty()) {
                null
            } else {
                HomeItem.Section(HomeSection(uuidNextUp, uiTextNextUp, nextUpItems))
            }

        _state.update { it.copy(nextUpSection = section) }
    }

    private suspend fun loadViews() {
        Timber.i("Loading views")
        val items =
            if (appPreferences.getValue(appPreferences.homeLatest)) {
                repository
                    .getUserViews()
                    .filter { view ->
                        CollectionType.fromString(view.collectionType?.serialName) in
                            CollectionType.supported
                    }
                    .let { views ->
                        // CGFLIX: últimos itens de cada biblioteca pedidos em paralelo
                        coroutineScope {
                            views
                                .map { view ->
                                    async { view to repository.getLatestMedia(view.id) }
                                }
                                .awaitAll()
                        }
                    }
                    .filter { (_, latest) -> latest.isNotEmpty() }
                    .map { (view, latest) -> view.toView(latest) }
                    .map { HomeItem.ViewItem(it) }
            } else {
                emptyList()
            }

        _state.update { it.copy(views = items) }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnRetryClick -> {
                loadData(force = true)
            }
            else -> Unit
        }
    }

    private companion object {
        const val REFRESH_MIN_INTERVAL_MS = 60_000L
    }
}
