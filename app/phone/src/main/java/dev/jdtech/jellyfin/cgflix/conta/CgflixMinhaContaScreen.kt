package dev.jdtech.jellyfin.cgflix.conta

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.CgflixButton
import dev.jdtech.jellyfin.presentation.theme.CgflixOutlinedButton
import dev.jdtech.jellyfin.presentation.theme.cgflix
import dev.jdtech.jellyfin.presentation.theme.spacings
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class CgflixMinhaContaState(
    val perfil: CgflixPerfil? = null,
    val ocupado: Boolean = false,
    val mensagem: String? = null,
    val erro: Boolean = false,
    /** Sobe a cada senha trocada com sucesso: a tela limpa os campos. */
    val senhasTrocadas: Int = 0,
)

@HiltViewModel
class CgflixMinhaContaViewModel @Inject constructor(private val repo: CgflixContaRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(CgflixMinhaContaState())
    val state = _state.asStateFlow()

    fun carregar() {
        viewModelScope.launch {
            try {
                val perfil = repo.perfil()
                _state.update { it.copy(perfil = perfil) }
            } catch (e: Exception) {
                Timber.e(e)
                aviso(SEM_CONEXAO, erro = true)
            }
        }
    }

    fun trocarFoto(uri: Uri) =
        executar(falha = "Não deu para trocar a foto. $CONFIRA") {
            repo.trocarFoto(uri)
            _state.update { it.copy(perfil = repo.perfil()) }
            aviso("Foto atualizada.")
        }

    fun removerFoto() =
        executar(falha = "Não deu para remover a foto. $CONFIRA") {
            repo.removerFoto()
            _state.update { it.copy(perfil = repo.perfil()) }
            aviso("Foto removida.")
        }

    fun trocarSenha(atual: String, nova: String, confirmar: String) {
        val problema =
            when {
                nova.isEmpty() -> "Digite a nova senha."
                nova != confirmar -> "A confirmação não bate com a nova senha."
                nova == atual -> "A nova senha é igual à atual."
                else -> null
            }
        if (problema != null) {
            aviso(problema, erro = true)
            return
        }
        executar(falha = "Não deu para trocar a senha. $CONFIRA") {
            when (repo.trocarSenha(atual, nova)) {
                CgflixSenhaResultado.OK -> {
                    _state.update { it.copy(senhasTrocadas = it.senhasTrocadas + 1) }
                    aviso(
                        "Senha trocada. Nos outros aparelhos e no site, entre de novo com a " +
                            "senha nova. Aqui você continua conectado."
                    )
                }
                CgflixSenhaResultado.SENHA_ATUAL_ERRADA ->
                    aviso(
                        "Senha atual incorreta. Cuidado: depois de algumas tentativas erradas, " +
                            "a conta é bloqueada e só o administrador desbloqueia.",
                        erro = true,
                    )
                CgflixSenhaResultado.FALHOU ->
                    aviso("Não deu para trocar a senha. $CONFIRA", erro = true)
            }
        }
    }

    private fun executar(falha: String, bloco: suspend () -> Unit) {
        if (_state.value.ocupado) return
        _state.update { it.copy(ocupado = true, mensagem = null) }
        viewModelScope.launch {
            try {
                bloco()
            } catch (e: Exception) {
                Timber.e(e)
                aviso(falha, erro = true)
            } finally {
                _state.update { it.copy(ocupado = false) }
            }
        }
    }

    private fun aviso(texto: String, erro: Boolean = false) {
        _state.update { it.copy(mensagem = texto, erro = erro) }
    }

    private companion object {
        const val CONFIRA = "Confira a internet e tente de novo."
        const val SEM_CONEXAO = "Sem conexão com o servidor agora. $CONFIRA"
    }
}

/** CGFLIX: Você → Minha conta (foto e senha do Jellyfin, pedido do Caio de 10/10). */
@Composable
fun CgflixMinhaContaScreen(
    navigateBack: () -> Unit,
    viewModel: CgflixMinhaContaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(true) { viewModel.carregar() }
    val galeria =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let(viewModel::trocarFoto)
        }
    CgflixMinhaContaLayout(
        state = state,
        navigateBack = navigateBack,
        onTrocarFoto = {
            galeria.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemoverFoto = viewModel::removerFoto,
        onTrocarSenha = viewModel::trocarSenha,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CgflixMinhaContaLayout(
    state: CgflixMinhaContaState,
    navigateBack: () -> Unit,
    onTrocarFoto: () -> Unit,
    onRemoverFoto: () -> Unit,
    onTrocarSenha: (atual: String, nova: String, confirmar: String) -> Unit,
) {
    val palette = MaterialTheme.cgflix
    // a chave limpa os campos depois de uma troca de senha que deu certo
    var atual by rememberSaveable(state.senhasTrocadas) { mutableStateOf("") }
    var nova by rememberSaveable(state.senhasTrocadas) { mutableStateOf("") }
    var confirmar by rememberSaveable(state.senhasTrocadas) { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Minha conta") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = "Voltar",
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.spacings.default),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            CgflixAvatar(
                nome = state.perfil?.nome.orEmpty(),
                fotoUrl = state.perfil?.fotoUrl,
                modifier = Modifier.size(112.dp),
            )
            Text(
                text = state.perfil?.nome.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                color = palette.text,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
                CgflixButton(onClick = onTrocarFoto, enabled = !state.ocupado) {
                    Text("Trocar foto")
                }
                if (state.perfil?.fotoUrl != null) {
                    CgflixOutlinedButton(onClick = onRemoverFoto, enabled = !state.ocupado) {
                        Text("Remover foto")
                    }
                }
            }

            state.mensagem?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.erro) MaterialTheme.colorScheme.error else palette.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacings.small))
            Text(
                text = "Trocar senha",
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                modifier = Modifier.fillMaxWidth(),
            )
            CampoSenha("Senha atual", atual) { atual = it }
            CampoSenha("Nova senha", nova) { nova = it }
            CampoSenha("Confirmar nova senha", confirmar) { confirmar = it }
            Text(
                text =
                    "A senha vale para o CGFLIX inteiro: app, site e TV. Depois da troca, os " +
                        "outros aparelhos pedem para entrar de novo.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted,
                modifier = Modifier.fillMaxWidth(),
            )
            CgflixButton(
                onClick = { onTrocarSenha(atual, nova, confirmar) },
                enabled = !state.ocupado && nova.isNotEmpty() && confirmar.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salvar nova senha")
            }
        }
    }
}

@Composable
private fun CampoSenha(rotulo: String, valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(rotulo) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Foto redonda da pessoa; sem foto, a inicial do nome sobre o roxo (ou verde) do tema. */
@Composable
fun CgflixAvatar(nome: String, fotoUrl: String?, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.cgflix
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clip(CircleShape).background(palette.accentDeep),
    ) {
        Text(
            text = nome.firstOrNull()?.uppercase() ?: "",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.onAccent,
        )
        if (fotoUrl != null) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = "Sua foto",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
