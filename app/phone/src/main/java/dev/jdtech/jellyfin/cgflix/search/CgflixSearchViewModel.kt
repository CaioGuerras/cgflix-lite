package dev.jdtech.jellyfin.cgflix.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.cgflix.CgflixRepository
import dev.jdtech.jellyfin.cgflix.logic.CgflixRequestRejected
import dev.jdtech.jellyfin.cgflix.logic.CgflixRequestState
import dev.jdtech.jellyfin.cgflix.logic.CgflixRequestable
import dev.jdtech.jellyfin.cgflix.maryanne.CgflixMaryanneRepository
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** Seção "Disponível para pedir". */
sealed interface CgflixRequestsSection {
    /** Sem busca ainda. */
    data object Idle : CgflixRequestsSection

    data object Loading : CgflixRequestsSection

    data class Results(val items: List<CgflixRequestable>) : CgflixRequestsSection

    /** Seerr fora do ar ou Quick Connect desligado: só um aviso pequeno. */
    data object Unavailable : CgflixRequestsSection
}

data class CgflixSearchState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<FindroidItem>? = null,
    val requests: CgflixRequestsSection = CgflixRequestsSection.Idle,
    /** tmdbIds com pedido em andamento (botão ocupado). */
    val requesting: Set<Int> = emptySet(),
)

/**
 * CGFLIX (Etapa 1B): uma busca só. Primeiro o que já temos (como no Findroid); embaixo, o que dá
 * para pedir no Seerr (filme pede direto, série pede todas as temporadas).
 */
@HiltViewModel
class CgflixSearchViewModel
@Inject
constructor(
    private val cgflix: CgflixRepository,
    private val repository: JellyfinRepository,
    private val maryanne: CgflixMaryanneRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CgflixSearchState())
    val state = _state.asStateFlow()

    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update {
                it.copy(results = null, loading = false, requests = CgflixRequestsSection.Idle)
            }
            return
        }
        searchJob =
            viewModelScope.launch(Dispatchers.Default) {
                delay(DEBOUNCE_MS) // espera a pessoa parar de digitar (internet ruim agradece)
                _state.update { it.copy(loading = true) }
                val ours =
                    try {
                        repository.getSearchItems(query.trim())
                    } catch (e: Exception) {
                        Timber.w(e, "CGFLIX: busca no servidor falhou")
                        emptyList()
                    }
                // CGFLIX (Modo Maryanne): criança não pede no Seerr; só o que já temos
                if (maryanne.isAtivo) {
                    _state.update {
                        it.copy(results = ours, loading = false, requests = CgflixRequestsSection.Idle)
                    }
                    return@launch
                }
                _state.update {
                    it.copy(
                        results = ours,
                        loading = false,
                        requests = CgflixRequestsSection.Loading,
                    )
                }
                val requests =
                    try {
                        CgflixRequestsSection.Results(cgflix.seerr().search(query.trim()))
                    } catch (e: Exception) {
                        // Sem detalhes da sessão no log
                        Timber.i("CGFLIX: pedidos indisponíveis (${e.message})")
                        CgflixRequestsSection.Unavailable
                    }
                _state.update { it.copy(requests = requests) }
            }
    }

    fun request(item: CgflixRequestable) {
        if (item.tmdbId in _state.value.requesting) return
        _state.update { it.copy(requesting = it.requesting + item.tmdbId) }
        viewModelScope.launch(Dispatchers.Default) {
            val message =
                try {
                    cgflix.seerr().request(item)
                    _state.update { current ->
                        val section = current.requests
                        if (section is CgflixRequestsSection.Results) {
                            current.copy(
                                requests =
                                    section.copy(
                                        items =
                                            section.items.map {
                                                if (
                                                    it.tmdbId == item.tmdbId &&
                                                        it.isMovie == item.isMovie
                                                ) {
                                                    it.copy(state = CgflixRequestState.REQUESTED)
                                                } else {
                                                    it
                                                }
                                            }
                                    )
                            )
                        } else {
                            current
                        }
                    }
                    "Pedido feito. Avisamos quando chegar."
                } catch (e: CgflixRequestRejected) {
                    e.message
                } catch (e: Exception) {
                    "Pedidos indisponíveis agora. Tente mais tarde."
                }
            _state.update { it.copy(requesting = it.requesting - item.tmdbId) }
            messages.send(message)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 450L
    }
}
