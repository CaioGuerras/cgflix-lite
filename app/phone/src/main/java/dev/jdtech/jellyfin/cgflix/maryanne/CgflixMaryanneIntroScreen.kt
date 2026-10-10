package dev.jdtech.jellyfin.cgflix.maryanne

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.CgflixButton
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.cgflix
import dev.jdtech.jellyfin.presentation.theme.spacings
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class CgflixMaryanneIntroState(
    /** Páginas mostradas: as 3 na primeira vez, só a da classificação depois do "Não mostrar de novo". */
    val paginas: List<Int> = listOf(0, 1, 2),
    val indice: Int = 0,
    val teto: CgflixMaryanneTeto = CgflixMaryanneTeto.LIVRE,
    val naoMostrar: Boolean = false,
    val ativando: Boolean = false,
    val erro: String? = null,
    /** A sessão já é a infantil: a navegação vai para a home nova. */
    val ativado: Boolean = false,
) {
    val pagina: Int
        get() = paginas[indice]

    val ultima: Boolean
        get() = indice == paginas.lastIndex
}

@HiltViewModel
class CgflixMaryanneIntroViewModel
@Inject
constructor(private val maryanne: CgflixMaryanneRepository) : ViewModel() {
    private val _state =
        MutableStateFlow(
            CgflixMaryanneIntroState(
                paginas = if (maryanne.introOk) listOf(1) else listOf(0, 1, 2),
                teto = maryanne.teto,
            )
        )
    val state = _state.asStateFlow()

    fun escolherTeto(teto: CgflixMaryanneTeto) = _state.update { it.copy(teto = teto, erro = null) }

    fun marcarNaoMostrar(valor: Boolean) = _state.update { it.copy(naoMostrar = valor) }

    /** Volta uma página; `false` quando já está na primeira (a tela fecha). */
    fun voltar(): Boolean {
        val atual = _state.value
        if (atual.ativando) return true
        if (atual.indice == 0) return false
        _state.update { it.copy(indice = it.indice - 1, erro = null) }
        return true
    }

    fun avancar() {
        val atual = _state.value
        if (atual.ativando) return
        if (!atual.ultima) {
            _state.update { it.copy(indice = it.indice + 1, erro = null) }
            return
        }
        _state.update { it.copy(ativando = true, erro = null) }
        viewModelScope.launch {
            try {
                maryanne.teto = atual.teto
                if (atual.naoMostrar) maryanne.introOk = true
                maryanne.ativar(atual.teto)
                _state.update { it.copy(ativando = false, ativado = true) }
            } catch (e: CgflixMaryanneErro) {
                _state.update { it.copy(ativando = false, erro = e.message) }
            } catch (e: Exception) {
                Timber.e(e, "CGFLIX: Modo Maryanne falhou")
                _state.update {
                    it.copy(ativando = false, erro = CgflixMaryanneRepository.mensagemDoCodigo(500))
                }
            }
        }
    }
}

/** CGFLIX (Modo Maryanne): apresentação antes de ativar (3 telas na 1ª vez). */
@Composable
fun CgflixMaryanneIntroScreen(
    navigateBack: () -> Unit,
    onAtivado: () -> Unit,
    viewModel: CgflixMaryanneIntroViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.ativado) {
        LaunchedEffect(true) { onAtivado() }
    }
    BackHandler { if (!viewModel.voltar()) navigateBack() }
    DisposableEffect(true) {
        maryanneIntroAberta.value = true
        onDispose { maryanneIntroAberta.value = false }
    }
    CgflixMaryanneIntroLayout(
        state = state,
        onFechar = navigateBack,
        onAvancar = viewModel::avancar,
        onTeto = viewModel::escolherTeto,
        onNaoMostrar = viewModel::marcarNaoMostrar,
    )
}

/** Já no tema Maryanne: a pessoa vê como o app vai ficar. */
@Composable
fun CgflixMaryanneIntroLayout(
    state: CgflixMaryanneIntroState,
    onFechar: () -> Unit,
    onAvancar: () -> Unit,
    onTeto: (CgflixMaryanneTeto) -> Unit,
    onNaoMostrar: (Boolean) -> Unit,
) {
    FindroidTheme(darkTheme = false, dynamicColor = false, maryanne = true) {
        val palette = MaterialTheme.cgflix
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(palette.background)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .testTag("maryanne_intro_${state.pagina + 1}")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacings.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onFechar, enabled = !state.ativando) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_x),
                        contentDescription = "Fechar",
                        tint = palette.textMuted,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (state.paginas.size > 1) {
                    Pontinhos(total = state.paginas.size, atual = state.indice)
                    Spacer(Modifier.width(MaterialTheme.spacings.medium))
                }
            }
            Column(
                modifier =
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(MaterialTheme.spacings.medium))
                when (state.pagina) {
                    0 -> PaginaApresentacao()
                    1 -> PaginaClassificacao(state.teto, enabled = !state.ativando, onTeto = onTeto)
                    else ->
                        PaginaPodeAssistir(
                            naoMostrar = state.naoMostrar,
                            enabled = !state.ativando,
                            onNaoMostrar = onNaoMostrar,
                        )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.erro?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier.padding(bottom = MaterialTheme.spacings.medium)
                                .testTag("maryanne_erro"),
                    )
                }
                CgflixButton(
                    onClick = onAvancar,
                    enabled = !state.ativando,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("maryanne_avancar"),
                ) {
                    if (state.ativando) {
                        CircularProgressIndicator(
                            color = palette.onAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(if (state.ultima) "Ativar o Modo Maryanne" else "Continuar")
                    }
                }
            }
        }
    }
}

@Composable
private fun Pontinhos(total: Int, atual: Int) {
    val palette = MaterialTheme.cgflix
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        repeat(total) { i ->
            Box(
                Modifier.size(if (i == atual) 10.dp else 8.dp)
                    .background(if (i == atual) palette.accent else palette.surfaceHighest, CircleShape)
            )
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.cgflix.text,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun Corpo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.cgflix.textMuted,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PaginaApresentacao() {
    Image(
        painter = painterResource(CoreR.drawable.ic_banner_maryanne),
        contentDescription = "CGFLIX",
        modifier = Modifier.width(240.dp),
    )
    Spacer(Modifier.height(MaterialTheme.spacings.large))
    Titulo("O CGFLIX virou um app para crianças")
    Spacer(Modifier.height(MaterialTheme.spacings.medium))
    Corpo(
        "No Modo Maryanne, o app mostra só filmes, séries e desenhos para crianças, com cores " +
            "e morangos. Fica mais simples: sem pedidos, sem configurações e sem trocar de conta."
    )
    Spacer(Modifier.height(MaterialTheme.spacings.medium))
    Corpo("Para voltar ao CGFLIX normal, é só ir em Você e tocar em \"Sair do Modo Maryanne\".")
}

@Composable
private fun PaginaClassificacao(
    teto: CgflixMaryanneTeto,
    enabled: Boolean,
    onTeto: (CgflixMaryanneTeto) -> Unit,
) {
    CgflixMorangoPulando(size = 72.dp)
    Spacer(Modifier.height(MaterialTheme.spacings.medium))
    Titulo("Até que idade?")
    Spacer(Modifier.height(MaterialTheme.spacings.medium))
    Corpo(
        "Só aparecem títulos com a classificação indicativa brasileira (ClassInd) que você " +
            "escolher. O que não tem classificação no servidor fica de fora."
    )
    Spacer(Modifier.height(MaterialTheme.spacings.large))
    Column(Modifier.fillMaxWidth().selectableGroup()) {
        OpcaoTeto(
            titulo = "Livre (recomendado)",
            detalhe = "Só o que é indicado para todas as idades.",
            selecionado = teto == CgflixMaryanneTeto.LIVRE,
            enabled = enabled,
            tag = "maryanne_teto_livre",
            onClick = { onTeto(CgflixMaryanneTeto.LIVRE) },
        )
        Spacer(Modifier.height(MaterialTheme.spacings.small))
        OpcaoTeto(
            titulo = "Até 10 anos",
            detalhe = "Livre e também o que é indicado a partir de 10 anos.",
            selecionado = teto == CgflixMaryanneTeto.DEZ,
            enabled = enabled,
            tag = "maryanne_teto_10",
            onClick = { onTeto(CgflixMaryanneTeto.DEZ) },
        )
    }
}

@Composable
private fun OpcaoTeto(
    titulo: String,
    detalhe: String,
    selecionado: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val palette = MaterialTheme.cgflix
    val forma = MaterialTheme.shapes.large
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(if (selecionado) palette.accentDeep else palette.surface, forma)
                .border(
                    width = if (selecionado) 2.dp else 1.dp,
                    color = if (selecionado) palette.accent else palette.surfaceHighest,
                    shape = forma,
                )
                .selectable(
                    selected = selecionado,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .padding(MaterialTheme.spacings.medium)
                .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selecionado, onClick = null, enabled = enabled)
        Spacer(Modifier.width(MaterialTheme.spacings.medium))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = palette.text)
            Text(detalhe, style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
        }
    }
}

@Composable
private fun PaginaPodeAssistir(naoMostrar: Boolean, enabled: Boolean, onNaoMostrar: (Boolean) -> Unit) {
    val palette = MaterialTheme.cgflix
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Image(
                painter = painterResource(CoreR.drawable.ic_cgflix_morango),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
        }
    }
    Spacer(Modifier.height(MaterialTheme.spacings.large))
    Titulo("Pode deixar seu filho assistir")
    Spacer(Modifier.height(MaterialTheme.spacings.medium))
    Corpo(
        "A criança vê só o que passou pelo filtro da classificação. A busca também segue o " +
            "filtro, e o que ela assistir fica separado do que você assiste."
    )
    Spacer(Modifier.height(MaterialTheme.spacings.large))
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .toggleable(
                    value = naoMostrar,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onNaoMostrar,
                )
                .padding(vertical = MaterialTheme.spacings.small)
                .testTag("maryanne_nao_mostrar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Checkbox(checked = naoMostrar, onCheckedChange = null, enabled = enabled)
        Spacer(Modifier.width(MaterialTheme.spacings.small))
        Text("Não mostrar de novo", color = palette.text)
    }
}
