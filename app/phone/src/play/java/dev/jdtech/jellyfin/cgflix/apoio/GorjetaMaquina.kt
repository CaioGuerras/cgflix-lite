package dev.jdtech.jellyfin.cgflix.apoio

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** CGFLIX (sabor `play`): os 3 produtos consumíveis do Play Console, na ordem da tela. */
val GORJETAS = listOf("gorjeta_pequena", "gorjeta_media", "gorjeta_grande")

enum class GorjetaResposta {
    OK,
    CANCELADO,
    SEM_REDE,
    INDISPONIVEL,
    JA_TEM,
    ERRO,
}

/** Preço e título vêm sempre da Play (`ProductDetails`), nunca do código. */
data class GorjetaProduto(val id: String, val titulo: String, val preco: String)

enum class GorjetaCompraEstado {
    CONCLUIDA,
    PENDENTE,
    OUTRO,
}

data class GorjetaCompra(val token: String, val estado: GorjetaCompraEstado)

data class GorjetaLista<T>(val resposta: GorjetaResposta, val itens: List<T> = emptyList())

/** O que a máquina usa da Play (o `BillingClient` de verdade está em [PlayGorjetaLoja]). */
interface GorjetaLoja {
    suspend fun conectar(): GorjetaResposta

    suspend fun produtos(ids: List<String>): GorjetaLista<GorjetaProduto>

    suspend fun comprasEmAberto(): GorjetaLista<GorjetaCompra>

    fun comprar(activity: Activity?, produtoId: String): GorjetaResposta

    suspend fun consumir(token: String): GorjetaResposta

    fun aoAtualizar(ouvinte: (GorjetaResposta, List<GorjetaCompra>) -> Unit)

    fun encerrar()
}

enum class GorjetaAviso {
    OBRIGADO,
    PENDENTE,
    CANCELADO,
    SEM_REDE,
    INDISPONIVEL,
}

sealed interface GorjetaEstado {
    data object Carregando : GorjetaEstado

    /** Produtos ainda não criados no Play Console. */
    data object EmBreve : GorjetaEstado

    data class Indisponivel(val semRede: Boolean) : GorjetaEstado

    data class Pronto(
        val produtos: List<GorjetaProduto>,
        val comprando: Boolean = false,
        val aviso: GorjetaAviso? = null,
    ) : GorjetaEstado
}

/**
 * CGFLIX (sabor `play`): fluxo da gorjeta. Conecta, consulta os produtos, abre a compra da Play e
 * **confirma e consome** toda compra concluída (consumir já confirma), inclusive as pendentes que
 * se concluem depois (ver [consumirEmAberto], chamado também ao abrir o app). Sem servidor de
 * verificação: a gorjeta não destrava nada.
 */
class GorjetaMaquina(private val loja: GorjetaLoja, private val escopo: CoroutineScope) {
    private val _estado = MutableStateFlow<GorjetaEstado>(GorjetaEstado.Carregando)
    val estado: StateFlow<GorjetaEstado> = _estado.asStateFlow()

    init {
        loja.aoAtualizar { resposta, compras ->
            escopo.launch { compraAtualizada(resposta, compras) }
        }
    }

    suspend fun iniciar() {
        _estado.value = GorjetaEstado.Carregando
        val conexao = loja.conectar()
        if (conexao != GorjetaResposta.OK) {
            _estado.value =
                GorjetaEstado.Indisponivel(semRede = conexao == GorjetaResposta.SEM_REDE)
            return
        }
        val produtos = loja.produtos(GORJETAS)
        _estado.value =
            when {
                produtos.resposta != GorjetaResposta.OK ->
                    GorjetaEstado.Indisponivel(
                        semRede = produtos.resposta == GorjetaResposta.SEM_REDE
                    )
                produtos.itens.isEmpty() -> GorjetaEstado.EmBreve
                else -> GorjetaEstado.Pronto(produtos.itens.sortedBy { GORJETAS.indexOf(it.id) })
            }
        consumirEmAberto()
    }

    /** Consome as compras concluídas que ficaram para trás. Devolve quantas foram consumidas. */
    suspend fun consumirEmAberto(): Int {
        val compras = loja.comprasEmAberto()
        if (compras.resposta != GorjetaResposta.OK) return 0
        return compras.itens.count {
            it.estado == GorjetaCompraEstado.CONCLUIDA &&
                loja.consumir(it.token) == GorjetaResposta.OK
        }
    }

    fun comprar(activity: Activity?, produtoId: String) {
        val atual = _estado.value as? GorjetaEstado.Pronto ?: return
        if (atual.comprando) return
        when (val resposta = loja.comprar(activity, produtoId)) {
            GorjetaResposta.OK -> _estado.value = atual.copy(comprando = true, aviso = null)
            // Gorjeta anterior ainda não consumida: consome e agradece; dá para apoiar de novo
            GorjetaResposta.JA_TEM -> escopo.launch { compraAtualizada(resposta, emptyList()) }
            else -> _estado.value = atual.copy(aviso = avisoDe(resposta))
        }
    }

    suspend fun compraAtualizada(resposta: GorjetaResposta, compras: List<GorjetaCompra>) {
        val aviso =
            when (resposta) {
                GorjetaResposta.OK -> {
                    var concluida = false
                    var pendente = false
                    for (compra in compras) {
                        when (compra.estado) {
                            GorjetaCompraEstado.CONCLUIDA -> {
                                // Se falhar (sem rede), fica para a próxima abertura do app
                                loja.consumir(compra.token)
                                concluida = true
                            }
                            GorjetaCompraEstado.PENDENTE -> pendente = true
                            GorjetaCompraEstado.OUTRO -> Unit
                        }
                    }
                    when {
                        concluida -> GorjetaAviso.OBRIGADO
                        pendente -> GorjetaAviso.PENDENTE
                        else -> null
                    }
                }
                GorjetaResposta.JA_TEM -> {
                    consumirEmAberto()
                    GorjetaAviso.OBRIGADO
                }
                else -> avisoDe(resposta)
            }
        _estado.update {
            if (it is GorjetaEstado.Pronto) it.copy(comprando = false, aviso = aviso) else it
        }
    }

    fun avisoVisto() {
        _estado.update { if (it is GorjetaEstado.Pronto) it.copy(aviso = null) else it }
    }

    fun encerrar() = loja.encerrar()

    private fun avisoDe(resposta: GorjetaResposta): GorjetaAviso? =
        when (resposta) {
            GorjetaResposta.OK -> null
            GorjetaResposta.CANCELADO -> GorjetaAviso.CANCELADO
            GorjetaResposta.SEM_REDE -> GorjetaAviso.SEM_REDE
            GorjetaResposta.JA_TEM -> GorjetaAviso.OBRIGADO
            GorjetaResposta.INDISPONIVEL,
            GorjetaResposta.ERRO -> GorjetaAviso.INDISPONIVEL
        }
}
