package dev.jdtech.jellyfin.cgflix.you

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.cgflix.apoio.CGFLIX_APOIO_TITULO
import dev.jdtech.jellyfin.cgflix.apoio.CgflixApoioRosa
import dev.jdtech.jellyfin.cgflix.apoio.apoioDoSabor
import dev.jdtech.jellyfin.cgflix.conta.CgflixAvatar
import dev.jdtech.jellyfin.cgflix.conta.CgflixContaRepository
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.presentation.theme.cgflix
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class CgflixYouState(
    val userName: String = "",
    val serverName: String = "",
    /** Foto do Jellyfin (Minha conta); nula sem foto ou sem rede. */
    val fotoUrl: String? = null,
)

@HiltViewModel
class CgflixYouViewModel
@Inject
constructor(
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
    private val conta: CgflixContaRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CgflixYouState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val serverId = appPreferences.getValue(appPreferences.currentServer) ?: return@launch
            val current = database.getServerWithAddressAndUser(serverId) ?: return@launch
            _state.value =
                CgflixYouState(
                    userName = current.user?.name ?: "",
                    serverName = current.server.name,
                )
            // a foto vem do servidor; sem rede fica a inicial do nome
            try {
                val fotoUrl = conta.perfil().fotoUrl
                _state.update { it.copy(fotoUrl = fotoUrl) }
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }
}

/**
 * CGFLIX (Etapa 1B): aba "Você", na ordem pedida pelo Caio (10/10): Favoritos, Bibliotecas e Meus
 * pedidos; depois Configurações, Trocar usuário e Sobre; por último, "Apoie o CGFLIX" em destaque.
 * A dedicatória fica só no Sobre. O topo (foto e nome) abre a Minha conta (foto e senha).
 */
@Composable
fun CgflixYouScreen(
    onMyRequests: () -> Unit,
    onLibraries: () -> Unit,
    onFavorites: () -> Unit,
    onSwitchUser: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onApoio: () -> Unit,
    onMinhaConta: () -> Unit = {},
    viewModel: CgflixYouViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(true) { viewModel.load() }
    CgflixYouLayout(
        state = state,
        offline = LocalOfflineMode.current,
        onMyRequests = onMyRequests,
        onLibraries = onLibraries,
        onFavorites = onFavorites,
        onSwitchUser = onSwitchUser,
        onSettings = onSettings,
        onAbout = onAbout,
        onApoio = onApoio,
        onMinhaConta = onMinhaConta,
    )
}

@Composable
fun CgflixYouLayout(
    state: CgflixYouState,
    offline: Boolean,
    onMyRequests: () -> Unit,
    onLibraries: () -> Unit,
    onFavorites: () -> Unit,
    onSwitchUser: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onApoio: () -> Unit,
    onMinhaConta: () -> Unit = {},
) {
    val safePadding = rememberSafePadding(handleStartInsets = false)
    val palette = MaterialTheme.cgflix

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = safePadding.top + MaterialTheme.spacings.medium,
                bottom = safePadding.bottom + MaterialTheme.spacings.default,
            ),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable(
                            enabled = !offline,
                            onClickLabel = "Minha conta",
                            role = Role.Button,
                            onClick = onMinhaConta,
                        )
                        .padding(horizontal = MaterialTheme.spacings.default)
                        .padding(bottom = MaterialTheme.spacings.medium),
            ) {
                CgflixAvatar(
                    nome = state.userName,
                    fotoUrl = state.fotoUrl.takeIf { !offline },
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.width(MaterialTheme.spacings.medium))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.userName.ifEmpty { "Você" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.text,
                    )
                    Text(
                        text = if (offline) state.serverName else "Minha conta: foto e senha",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                    )
                }
                if (!offline) {
                    Icon(
                        painterResource(CoreR.drawable.ic_cgflix_chevron_right),
                        contentDescription = null,
                        tint = palette.textMuted,
                    )
                }
            }
        }
        if (!offline) {
            item { YouItem(CoreR.drawable.ic_cgflix_favorite, "Favoritos", onFavorites) }
            item { YouItem(CoreR.drawable.ic_cgflix_video_library, "Bibliotecas", onLibraries) }
            item { YouItem(CoreR.drawable.ic_cgflix_list_alt, "Meus pedidos", onMyRequests) }
            item { HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacings.small)) }
        }
        item { YouItem(CoreR.drawable.ic_cgflix_settings, "Configurações", onSettings) }
        item { YouItem(CoreR.drawable.ic_cgflix_switch_account, "Trocar usuário", onSwitchUser) }
        item { YouItem(CoreR.drawable.ic_cgflix_info, "Sobre", onAbout) }
        if (apoioDoSabor.disponivel()) {
            item { HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacings.small)) }
            item {
                YouItem(
                    CoreR.drawable.ic_cgflix_heart,
                    CGFLIX_APOIO_TITULO,
                    onApoio,
                    iconTint = CgflixApoioRosa,
                )
            }
        }
    }
}

@Composable
private fun YouItem(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.cgflix.lilac,
) {
    val palette = MaterialTheme.cgflix
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
                .padding(horizontal = MaterialTheme.spacings.default),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = iconTint)
        Spacer(Modifier.width(MaterialTheme.spacings.medium))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(
            painterResource(CoreR.drawable.ic_cgflix_chevron_right),
            contentDescription = null,
            tint = palette.textMuted,
        )
    }
}
