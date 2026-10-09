package dev.jdtech.jellyfin.cgflix.apoio

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.CgflixOutlinedButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/** CGFLIX (sabor `play`): gorjeta só pelo Google Play Billing (política de Pagamentos da Play). */
val apoioDoSabor: Apoio = ApoioPlay

private object ApoioPlay : Apoio {
    override fun disponivel() = true

    override fun aoAbrirApp(context: Context) {
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            // Depois da primeira tela, para não pesar na abertura
            delay(5_000)
            val loja = PlayGorjetaLoja(app)
            try {
                if (loja.conectar() == GorjetaResposta.OK) {
                    GorjetaMaquina(loja, this).consumirEmAberto()
                }
            } catch (e: Exception) {
                Timber.w(e, "Gorjeta: falha ao conferir compras em aberto")
            } finally {
                loja.encerrar()
            }
        }
    }

    @Composable
    override fun Abrir(demonstracao: Boolean) {
        val context = LocalContext.current
        val escopo = rememberCoroutineScope()
        val maquina = remember {
            GorjetaMaquina(if (demonstracao) LojaDemonstracao else PlayGorjetaLoja(context), escopo)
        }
        DisposableEffect(maquina) {
            escopo.launch { maquina.iniciar() }
            onDispose { maquina.encerrar() }
        }
        val estado by maquina.estado.collectAsState()

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val atual = estado) {
                GorjetaEstado.Carregando -> CircularProgressIndicator(Modifier.size(32.dp))
                GorjetaEstado.EmBreve -> Aviso("Em breve você vai poder apoiar por aqui.")
                is GorjetaEstado.Indisponivel -> {
                    Aviso(
                        if (atual.semRede) "Apoio indisponível no momento. Confira a internet."
                        else "Apoio indisponível no momento."
                    )
                    CgflixOutlinedButton(onClick = { escopo.launch { maquina.iniciar() } }) {
                        Text("Tentar de novo")
                    }
                }
                is GorjetaEstado.Pronto -> {
                    atual.produtos.forEach { produto ->
                        CgflixOutlinedButton(
                            onClick = {
                                maquina.avisoVisto()
                                maquina.comprar(context.activity(), produto.id)
                            },
                            enabled = !atual.comprando,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${produto.titulo} · ${produto.preco}")
                        }
                    }
                    atual.aviso?.let { Aviso(textoDo(it), destaque = it == GorjetaAviso.OBRIGADO) }
                }
            }
        }
    }
}

@Composable
private fun Aviso(texto: String, destaque: Boolean = false) {
    Text(
        text = texto,
        style =
            if (destaque) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodyMedium,
        color =
            if (destaque) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private fun textoDo(aviso: GorjetaAviso) =
    when (aviso) {
        GorjetaAviso.OBRIGADO -> "Obrigado pelo apoio!"
        GorjetaAviso.PENDENTE ->
            "Pagamento pendente. Quando o Google Play confirmar, está tudo certo. Obrigado!"
        GorjetaAviso.CANCELADO -> "Compra cancelada."
        GorjetaAviso.SEM_REDE -> "Sem internet agora. Tente de novo."
        GorjetaAviso.INDISPONIVEL -> "Apoio indisponível no momento."
    }

private fun Context.activity(): Activity? {
    var atual: Context = this
    while (atual is ContextWrapper) {
        if (atual is Activity) return atual
        atual = atual.baseContext
    }
    return null
}

/** Só para as capturas do CI (tela de demonstração): preços de exemplo, nada é cobrado. */
private object LojaDemonstracao : GorjetaLoja {
    override suspend fun conectar() = GorjetaResposta.OK

    override suspend fun produtos(ids: List<String>) =
        GorjetaLista(
            GorjetaResposta.OK,
            listOf(
                GorjetaProduto("gorjeta_pequena", "Gorjeta pequena", "R$ 5,00"),
                GorjetaProduto("gorjeta_media", "Gorjeta média", "R$ 10,00"),
                GorjetaProduto("gorjeta_grande", "Gorjeta grande", "R$ 25,00"),
            ),
        )

    override suspend fun comprasEmAberto() = GorjetaLista<GorjetaCompra>(GorjetaResposta.OK)

    override fun comprar(activity: Activity?, produtoId: String) = GorjetaResposta.ERRO

    override suspend fun consumir(token: String) = GorjetaResposta.OK

    override fun aoAtualizar(ouvinte: (GorjetaResposta, List<GorjetaCompra>) -> Unit) = Unit

    override fun encerrar() = Unit
}
