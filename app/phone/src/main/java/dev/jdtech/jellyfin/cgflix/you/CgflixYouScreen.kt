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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import dev.jdtech.jellyfin.cgflix.maryanne.CgflixMaryanneRepository
import dev.jdtech.jellyfin.cgflix.maryanne.LocalCgflixMaryanne
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
    /** Classificação do Modo Maryanne ("Livre" ou "Até 10 anos"). */
    val maryanneTeto: String = "",
    val saindo: Boolean = false,
)

@HiltViewModel
class CgflixYouViewModel
@Inject
constructor(
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
    private val conta: CgflixContaRepository,
    private val maryanne: CgflixMaryanneRepository,
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
                    maryanneTeto = maryanne.teto.rotulo,
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

    /** Sai do Modo Maryanne; [onFim] recebe `true` quando voltou para a conta do pai. */
    fun sairDoMaryanne(onFim: (Boolean) -> Unit) {
        if (_state.value.saindo) return
        _state.update { it.copy(saindo = true) }
        viewModelScope.launch {
            val voltou =
                try {
                    maryanne.sair()
                } catch (e: Exception) {
                    Timber.e(e, "CGFLIX: sair do Modo Maryanne falhou")
                    maryanne.fecharCobertura()
                    false
                }
            _state.update { it.copy(saindo = false) }
            onFim(voltou)
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
    onMaryanne: () -> Unit = {},
    onMaryanneSaiu: (voltouParaOPai: Boolean) -> Unit = {},
    viewModel: CgflixYouViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(true) { viewModel.load() }
    var confirmarSaida by rememberSaveable { mutableStateOf(false) }
    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Sair do Modo Maryanne?") },
            text = { Text("O app volta a mostrar todo o CGFLIX, na sua conta.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarSaida = false
                        viewModel.sairDoMaryanne(onMaryanneSaiu)
                    },
                    modifier = Modifier.testTag("maryanne_sair_confirmar"),
                ) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSaida = false }) { Text("Continuar no modo") }
            },
        )
    }
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
        maryanne = LocalCgflixMaryanne.current,
        onMaryanne = onMaryanne,
        onSairMaryanne = { if (!state.saindo) confirmarSaida = true },
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
    maryanne: Boolean = false,
    onMaryanne: () -> Unit = {},
    onSairMaryanne: () -> Unit = {},
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
                            // CGFLIX (Modo Maryanne): sem Minha conta no usuário infantil
                            enabled = !offline && !maryanne,
                            onClickLabel = "Minha conta",
                            role = Role.Button,
                            onClick = onMinhaConta,
                        )
                        .padding(horizontal = MaterialTheme.spacings.default)
                        .padding(bottom = MaterialTheme.spacings.medium),
            ) {
                CgflixAvatar(
                    nome = state.userName,
                    fotoUrl = state.fotoUrl.takeIf { !offline && !maryanne },
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
                        text =
                            when {
                                maryanne -> "Modo Maryanne: ${state.maryanneTeto}"
                                offline -> state.serverName
                                else -> "Minha conta: foto e senha"
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                    )
                }
                if (!offline && !maryanne) {
                    Icon(
                        painterResource(CoreR.drawable.ic_cgflix_chevron_right),
                        contentDescription = null,
                        tint = palette.textMuted,
                    )
                }
            }
        }
        // CGFLIX (Modo Maryanne): sem pedidos, configurações, troca de usuário e Apoie no modo
        if (maryanne) {
            if (!offline) {
                item { YouItem(CoreR.drawable.ic_cgflix_favorite, "Favoritos", onFavorites) }
                item { YouItem(CoreR.drawable.ic_cgflix_video_library, "Bibliotecas", onLibraries) }
                item {
                    HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacings.small))
                }
            }
            item {
                YouItem(
                    CoreR.drawable.ic_cgflix_morango,
                    "Sair do Modo Maryanne",
                    onSairMaryanne,
                    iconTint = Color.Unspecified,
                    tag = "maryanne_sair",
                )
            }
            item { YouItem(CoreR.drawable.ic_cgflix_info, "Sobre", onAbout) }
            return@LazyColumn
        }
        if (!offline) {
            item { YouItem(CoreR.drawable.ic_cgflix_favorite, "Favoritos", onFavorites) }
            item { YouItem(CoreR.drawable.ic_cgflix_video_library, "Bibliotecas", onLibraries) }
            item { YouItem(CoreR.drawable.ic_cgflix_list_alt, "Meus pedidos", onMyRequests) }
            item {
                YouItem(
                    CoreR.drawable.ic_cgflix_morango,
                    "Modo Maryanne (crianças)",
                    onMaryanne,
                    iconTint = Color.Unspecified,
                    tag = "maryanne_entrar",
                )
            }
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
    tag: String? = null,
) {
    val palette = MaterialTheme.cgflix
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
                .padding(horizontal = MaterialTheme.spacings.default)
                .then(if (tag != null) Modifier.testTag(tag) else Modifier),
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
