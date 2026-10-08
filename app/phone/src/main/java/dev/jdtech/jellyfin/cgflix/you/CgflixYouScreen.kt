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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.launch

data class CgflixYouState(val userName: String = "", val serverName: String = "")

@HiltViewModel
class CgflixYouViewModel
@Inject
constructor(private val database: ServerDatabaseDao, private val appPreferences: AppPreferences) :
    ViewModel() {
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
        }
    }
}

/**
 * CGFLIX (Etapa 1B): aba "Você": trocar usuário, Meus pedidos, Configurações e Sobre (mais
 * Bibliotecas e Favoritos, que saíram da barra). A dedicatória fica só no Sobre.
 */
@Composable
fun CgflixYouScreen(
    onMyRequests: () -> Unit,
    onLibraries: () -> Unit,
    onFavorites: () -> Unit,
    onSwitchUser: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
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
            Column(Modifier.padding(horizontal = MaterialTheme.spacings.default)) {
                Text(
                    text = state.userName.ifEmpty { "Você" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.text,
                )
                if (state.serverName.isNotEmpty()) {
                    Text(
                        text = state.serverName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                    )
                }
                Spacer(Modifier.padding(top = MaterialTheme.spacings.medium))
            }
        }
        if (!offline) {
            item { YouItem(CoreR.drawable.ic_cgflix_list_alt, "Meus pedidos", onMyRequests) }
            item { YouItem(CoreR.drawable.ic_cgflix_video_library, "Bibliotecas", onLibraries) }
            item { YouItem(CoreR.drawable.ic_cgflix_favorite, "Favoritos", onFavorites) }
        }
        item { YouItem(CoreR.drawable.ic_cgflix_switch_account, "Trocar usuário", onSwitchUser) }
        item { HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacings.small)) }
        item { YouItem(CoreR.drawable.ic_cgflix_settings, "Configurações", onSettings) }
        item { YouItem(CoreR.drawable.ic_cgflix_info, "Sobre", onAbout) }
    }
}

@Composable
private fun YouItem(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    val palette = MaterialTheme.cgflix
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
                .padding(horizontal = MaterialTheme.spacings.default),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = palette.lilac)
        Spacer(Modifier.width(MaterialTheme.spacings.medium))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(
            painterResource(CoreR.drawable.ic_cgflix_chevron_right),
            contentDescription = null,
            tint = palette.textMuted,
        )
    }
}
