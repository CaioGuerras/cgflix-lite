package dev.jdtech.jellyfin.cgflix.title

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.cgflix.CgflixRepository
import dev.jdtech.jellyfin.cgflix.logic.CgflixLanguageBadges
import dev.jdtech.jellyfin.cgflix.logic.CgflixStreamInfo
import dev.jdtech.jellyfin.cgflix.logic.CgflixStreamKind
import dev.jdtech.jellyfin.cgflix.logic.cgflixLanguageBadges
import dev.jdtech.jellyfin.cgflix.ui.CgflixChipsRow
import dev.jdtech.jellyfin.cgflix.ui.CgflixLanguageBadgesRow
import dev.jdtech.jellyfin.cgflix.ui.CgflixSectionTitle
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ItemPoster
import dev.jdtech.jellyfin.presentation.theme.cgflix
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber

// CGFLIX (Etapa 1B): página do título. Selos Dublado/Legendado, texto do botão principal,
// temporadas em chips com o próximo episódio já selecionado e a música tema (desligada por padrão).

/** Selos a partir das faixas de áudio e legenda do item. */
fun FindroidItem.cgflixBadges(): CgflixLanguageBadges =
    cgflixLanguageBadges(
        sources
            .flatMap { it.mediaStreams }
            .map {
                CgflixStreamInfo(
                    kind =
                        when (it.type) {
                            MediaStreamType.AUDIO -> CgflixStreamKind.AUDIO
                            MediaStreamType.SUBTITLE -> CgflixStreamKind.SUBTITLE
                            else -> CgflixStreamKind.OTHER
                        },
                    language = it.language,
                    title = it.displayTitle ?: it.title,
                )
            }
    )

/** "Continuar S01E03" quando a pessoa já começou a série; senão "Assistir". */
fun cgflixShowPlayLabel(nextUp: FindroidEpisode?): String {
    if (nextUp == null) return "Assistir"
    val started =
        nextUp.playbackPositionTicks > 0 || nextUp.indexNumber > 1 || nextUp.parentIndexNumber > 1
    return if (started) {
        "Continuar S%02dE%02d".format(nextUp.parentIndexNumber, nextUp.indexNumber)
    } else {
        "Assistir"
    }
}

data class CgflixShowExtrasState(
    val selectedSeasonId: UUID? = null,
    val episodes: Map<UUID, List<FindroidEpisode>> = emptyMap(),
    val nextUpId: UUID? = null,
    val badges: CgflixLanguageBadges? = null,
)

@HiltViewModel
class CgflixShowExtrasViewModel
@Inject
constructor(
    private val cgflix: CgflixRepository,
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(CgflixShowExtrasState())
    val state = _state.asStateFlow()

    private var seriesId: UUID? = null
    private var player: MediaPlayer? = null
    private var themeTried = false

    fun load(seriesId: UUID, seasons: List<FindroidSeason>, nextUp: FindroidEpisode?) {
        if (seasons.isEmpty()) return
        this.seriesId = seriesId
        val current = _state.value
        // O próximo episódio já vem selecionado (a temporada dele)
        val selected =
            current.selectedSeasonId?.takeIf { id -> seasons.any { it.id == id } }
                ?: nextUp?.seasonId?.takeIf { id -> seasons.any { it.id == id } }
                ?: seasons.first().id
        _state.update { it.copy(selectedSeasonId = selected, nextUpId = nextUp?.id) }
        loadEpisodes(selected)
        if (current.badges == null) {
            viewModelScope.launch(Dispatchers.IO) {
                val episodeId =
                    nextUp?.id ?: runCatching { episodesOf(selected).firstOrNull()?.id }.getOrNull()
                val badges = episodeId?.let {
                    runCatching { repository.getEpisode(it).cgflixBadges() }.getOrNull()
                }
                _state.update { it.copy(badges = badges) }
            }
        }
    }

    fun selectSeason(seasonId: UUID) {
        _state.update { it.copy(selectedSeasonId = seasonId) }
        loadEpisodes(seasonId)
    }

    private fun loadEpisodes(seasonId: UUID) {
        if (_state.value.episodes.containsKey(seasonId)) return
        viewModelScope.launch(Dispatchers.IO) {
            val episodes =
                try {
                    episodesOf(seasonId)
                } catch (e: Exception) {
                    Timber.w(e, "CGFLIX: episódios não carregaram")
                    return@launch
                }
            _state.update { it.copy(episodes = it.episodes + (seasonId to episodes)) }
        }
    }

    private suspend fun episodesOf(seasonId: UUID): List<FindroidEpisode> {
        _state.value.episodes[seasonId]?.let {
            return it
        }
        return repository.getEpisodes(seriesId = seriesId!!, seasonId = seasonId)
    }

    /** Música tema: só se a pessoa ligou em Configurações (desligada por padrão no Lite). */
    fun startThemeMusic(seriesId: UUID) {
        if (themeTried || !appPreferences.getValue(appPreferences.cgflixThemeMusic)) return
        themeTried = true
        viewModelScope.launch(Dispatchers.IO) {
            val tvdb = cgflix.tvdbIdOf(seriesId) ?: return@launch
            val url = cgflix.themeMusicUrl(tvdb)
            launch(Dispatchers.Main) {
                try {
                    player =
                        MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            setDataSource(url)
                            setVolume(0.35f, 0.35f)
                            // 404 (série sem música) cai aqui e fica em silêncio
                            setOnErrorListener { mp, _, _ ->
                                mp.release()
                                if (player === mp) player = null
                                true
                            }
                            setOnPreparedListener { it.start() }
                            prepareAsync()
                        }
                } catch (e: Exception) {
                    Timber.i("CGFLIX: sem música tema (${e.javaClass.simpleName})")
                }
            }
        }
    }

    fun stopThemeMusic() {
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
    }

    override fun onCleared() {
        stopThemeMusic()
    }
}

/**
 * Selos, temporadas em chips e os episódios da temporada escolhida (o próximo destacado). Usado na
 * página da série no lugar da fileira de capas de temporada do Findroid.
 */
@Composable
fun CgflixShowExtras(
    seriesId: UUID,
    seasons: List<FindroidSeason>,
    nextUp: FindroidEpisode?,
    contentPadding: PaddingValues,
    onEpisodeClick: (FindroidItem) -> Unit,
    viewModel: CgflixShowExtrasViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(seriesId, seasons, nextUp?.id) { viewModel.load(seriesId, seasons, nextUp) }

    // Música tema: toca com a tela aberta e para ao sair dela ou ao ir para segundo plano
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, seriesId) {
        viewModel.startThemeMusic(seriesId)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.stopThemeMusic()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopThemeMusic()
        }
    }

    if (seasons.isEmpty()) return
    Column {
        CgflixLanguageBadgesRow(state.badges, Modifier.padding(contentPadding))
        if (state.badges?.isEmpty == false) Spacer(Modifier.height(MaterialTheme.spacings.medium))
        CgflixSectionTitle("Temporadas", Modifier.padding(contentPadding))
        Spacer(Modifier.height(MaterialTheme.spacings.small))
        val selectedIndex = seasons.indexOfFirst { it.id == state.selectedSeasonId }
        CgflixChipsRow(
            labels = seasons.map { it.name },
            onClick = { index -> viewModel.selectSeason(seasons[index].id) },
            contentPadding = contentPadding,
            selectedIndex = selectedIndex.takeIf { it >= 0 },
        )
        Spacer(Modifier.height(MaterialTheme.spacings.small))
        val episodes = state.selectedSeasonId?.let { state.episodes[it] }.orEmpty()
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            for (episode in episodes) {
                CgflixEpisodeRow(
                    episode = episode,
                    highlighted = episode.id == state.nextUpId,
                    onClick = { onEpisodeClick(episode) },
                )
            }
        }
    }
}

@Composable
private fun CgflixEpisodeRow(episode: FindroidEpisode, highlighted: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.cgflix
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .then(if (highlighted) Modifier.background(palette.accentDeep) else Modifier)
                .clickable(onClick = onClick)
                .semantics { selected = highlighted }
                .padding(4.dp),
    ) {
        ItemPoster(
            item = episode,
            direction = Direction.HORIZONTAL,
            modifier = Modifier.width(128.dp).clip(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.width(MaterialTheme.spacings.medium))
        Column(Modifier.weight(1f)) {
            Text(
                text = "${episode.indexNumber}. ${episode.name}",
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal
                    ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val minutes = episode.runtimeTicks / 600_000_000
            val info =
                listOfNotNull(
                    if (highlighted) "Próximo" else null,
                    if (episode.played) "Visto" else null,
                    if (minutes > 0) "$minutes min" else null,
                )
            if (info.isNotEmpty()) {
                Text(
                    text = info.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highlighted) palette.lilac else palette.textMuted,
                )
            }
        }
    }
}

/** Selos do próprio item (filme ou episódio), com o espaço de baixo; nada se não houver. */
@Composable
fun CgflixItemBadges(item: FindroidItem) {
    val badges = remember(item) { item.cgflixBadges() }
    if (badges.isEmpty) return
    CgflixLanguageBadgesRow(badges)
    Spacer(Modifier.height(MaterialTheme.spacings.small))
}
