package dev.jdtech.jellyfin.cgflix.demo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import dev.jdtech.jellyfin.cgflix.apoio.CgflixApoioScreen
import dev.jdtech.jellyfin.cgflix.home.CgflixCategoryLayout
import dev.jdtech.jellyfin.cgflix.home.CgflixCategoryState
import dev.jdtech.jellyfin.cgflix.home.CgflixHomeLayout
import dev.jdtech.jellyfin.cgflix.home.CgflixHomeState
import dev.jdtech.jellyfin.cgflix.home.CgflixRowState
import dev.jdtech.jellyfin.cgflix.logic.CgflixCategory
import dev.jdtech.jellyfin.cgflix.logic.CgflixLibraryInfo
import dev.jdtech.jellyfin.cgflix.logic.CgflixRequestState
import dev.jdtech.jellyfin.cgflix.logic.CgflixRequestable
import dev.jdtech.jellyfin.cgflix.logic.CgflixRowSpec
import dev.jdtech.jellyfin.cgflix.logic.cgflixCategoryRows
import dev.jdtech.jellyfin.cgflix.logic.cgflixHomeRows
import dev.jdtech.jellyfin.cgflix.search.CgflixRequestsSection
import dev.jdtech.jellyfin.cgflix.search.CgflixSearchLayout
import dev.jdtech.jellyfin.cgflix.search.CgflixSearchState
import dev.jdtech.jellyfin.cgflix.ui.CgflixOpening
import dev.jdtech.jellyfin.cgflix.you.CgflixYouLayout
import dev.jdtech.jellyfin.cgflix.you.CgflixYouState
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.core.presentation.dummy.dummyShow
import dev.jdtech.jellyfin.core.presentation.dummy.dummyVideoMetadata
import dev.jdtech.jellyfin.core.presentation.theme.CgflixThemeChoice
import dev.jdtech.jellyfin.film.presentation.movie.MovieState
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.film.MovieScreenLayout
import dev.jdtech.jellyfin.presentation.settings.SettingsScreenLayout
import dev.jdtech.jellyfin.presentation.setup.login.LoginScreenLayout
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.presentation.enums.DeviceType
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsViewModel
import dev.jdtech.jellyfin.setup.presentation.login.LoginState
import java.util.UUID
import kotlinx.coroutines.flow.flowOf

/**
 * CGFLIX (só no build de debug): mostra as telas com dados falsos, sem servidor, para o CI tirar
 * capturas no emulador. `adb shell am start -n <pacote>/dev.jdtech.jellyfin.cgflix.demo.
 * CgflixDemoActivity --es tela inicio|filmes|series|animes|busca|voce|abertura|detalhes|
 * configuracoes|login|apoio --es tema isis|heitor`.
 */
class CgflixDemoActivity : ComponentActivity() {
    private val movies =
        listOf(
                "Ainda Estou Aqui",
                "Central do Brasil",
                "Cidade de Deus",
                "O Auto da Compadecida",
                "Tropa de Elite",
                "Bacurau",
            )
            .map { dummyMovie.copy(id = UUID.randomUUID(), name = it) }
    private val shows =
        listOf("Sintonia", "Cidade Invisível", "DNA do Crime", "Irmandade", "Bom Dia, Verônica")
            .map { dummyShow.copy(id = UUID.randomUUID(), name = it) }
    private val animes =
        listOf("One Piece", "Naruto", "Frieren", "Jujutsu Kaisen", "Spy x Family").map {
            dummyShow.copy(id = UUID.randomUUID(), name = it)
        }

    private val libraries =
        mapOf(
            CgflixCategory.FILMES to CgflixLibraryInfo("f", "Filmes", "movies"),
            CgflixCategory.SERIES to CgflixLibraryInfo("s", "Séries", "tvshows"),
            CgflixCategory.ANIMES to CgflixLibraryInfo("a", "Animes", "tvshows"),
        )

    private fun itemsOf(category: CgflixCategory?): List<FindroidItem> =
        when (category) {
            CgflixCategory.FILMES -> movies
            CgflixCategory.SERIES -> shows
            CgflixCategory.ANIMES -> animes
            null -> movies
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("tela") ?: "inicio"
        val dark = CgflixThemeChoice.from(intent.getStringExtra("tema")).isDark(systemIsDark = true)
        val barStyle =
            if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        setContent {
            FindroidTheme(darkTheme = dark, dynamicColor = false) {
                when (screen) {
                    "abertura" -> CgflixOpening()
                    // Gorjeta: Pix no `libre`; no `play`, produtos de exemplo (nada é cobrado)
                    "apoio" -> CgflixApoioScreen(navigateBack = {}, demonstracao = true)
                    "detalhes" ->
                        MovieScreenLayout(
                            state =
                                MovieState(
                                    movie = movies[0],
                                    videoMetadata = dummyVideoMetadata,
                                ),
                            downloaderState = DownloaderState(),
                            onAction = {},
                            onDownloaderAction = {},
                        )
                    "configuracoes" -> {
                        val settings = remember { demoSettings(dark) }
                        val settingsState by settings.state.collectAsState()
                        SettingsScreenLayout(
                            title = SettingsR.string.settings_category_interface,
                            state = settingsState,
                            onAction = {},
                        )
                    }
                    "login" ->
                        LoginScreenLayout(
                            state = LoginState(serverName = "CGFLIX"),
                            onAction = {},
                        )
                    "busca" -> {
                        val snackbar = remember { SnackbarHostState() }
                        CgflixSearchLayout(
                            state = searchState(),
                            snackbar = snackbar,
                            onQueryChange = {},
                            onItemClick = {},
                            onRequest = {},
                        )
                    }
                    "voce" ->
                        CgflixYouLayout(
                            state = CgflixYouState(userName = "Isis", serverName = "CGFLIX"),
                            offline = false,
                            onMyRequests = {},
                            onLibraries = {},
                            onFavorites = {},
                            onSwitchUser = {},
                            onSettings = {},
                            onAbout = {},
                            onApoio = {},
                        )
                    "filmes",
                    "series",
                    "animes" -> {
                        val category =
                            when (screen) {
                                "filmes" -> CgflixCategory.FILMES
                                "series" -> CgflixCategory.SERIES
                                else -> CgflixCategory.ANIMES
                            }
                        val all = remember { flowOf(PagingData.from(itemsOf(category))) }
                        CgflixCategoryLayout(
                            category = category,
                            state = categoryState(category),
                            items = all.collectAsLazyPagingItems(),
                            onItemClick = {},
                            navigateBack = {},
                        )
                    }
                    else ->
                        CgflixHomeLayout(
                            state = homeState(),
                            onRefresh = {},
                            onCategoryClick = {},
                            onItemClick = {},
                        )
                }
            }
        }
    }

    /** Configurações > Interface de verdade (com o grupo Aparência), em preferências só da demo. */
    private fun demoSettings(dark: Boolean): SettingsViewModel {
        val prefs = AppPreferences(getSharedPreferences("cgflix_demo", MODE_PRIVATE))
        prefs.setValue(prefs.cgflixTheme, if (dark) "isis" else "heitor")
        return SettingsViewModel(prefs).also {
            it.loadPreferences(
                intArrayOf(SettingsR.string.settings_category_interface),
                DeviceType.PHONE,
            )
        }
    }

    private fun homeState(): CgflixHomeState {
        val rows = cgflixHomeRows(libraries)
        return CgflixHomeState(
            chips = libraries.toList(),
            rows = rows,
            rowStates =
                rows.associate { row ->
                    row.key to
                        when (row.kind) {
                            CgflixRowSpec.Kind.CONTINUE ->
                                CgflixRowState.Ready(
                                    listOf(
                                        dummyEpisode.copy(
                                            id = UUID.randomUUID(),
                                            seriesName = "Sintonia",
                                        ),
                                        dummyEpisode.copy(
                                            id = UUID.randomUUID(),
                                            seriesName = "One Piece",
                                        ),
                                    )
                                )
                            CgflixRowSpec.Kind.TRENDING ->
                                CgflixRowState.Ready(
                                    listOf(movies[0], animes[2], shows[1], movies[4], animes[0])
                                )
                            CgflixRowSpec.Kind.RECENT -> CgflixRowState.Ready(itemsOf(row.category))
                        }
                },
        )
    }

    private fun categoryState(category: CgflixCategory): CgflixCategoryState {
        val rows = cgflixCategoryRows(category, libraries.getValue(category))
        return CgflixCategoryState(
            category = category,
            library = libraries[category],
            rows = rows,
            rowStates = rows.associate { it.key to CgflixRowState.Ready(itemsOf(category)) },
        )
    }

    private fun searchState() =
        CgflixSearchState(
            query = "cidade",
            results = listOf(movies[2], shows[1]),
            requests =
                CgflixRequestsSection.Results(
                    listOf(
                        CgflixRequestable(1, true, "Cidade dos Homens", 2007),
                        CgflixRequestable(
                            2,
                            false,
                            "Cidade Invisível: Especial",
                            2023,
                            state = CgflixRequestState.REQUESTED,
                        ),
                        CgflixRequestable(
                            3,
                            true,
                            "Cidade Baixa",
                            2005,
                            state = CgflixRequestState.DOWNLOADING,
                        ),
                    )
                ),
        )
}
