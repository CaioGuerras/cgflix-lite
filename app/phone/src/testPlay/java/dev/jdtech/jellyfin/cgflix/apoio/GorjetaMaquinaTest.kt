package dev.jdtech.jellyfin.cgflix.apoio

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** CGFLIX: máquina de estados da gorjeta com uma loja falsa no lugar do `BillingClient`. */
class GorjetaMaquinaTest {
    private class LojaFalsa(
        var conexao: GorjetaResposta = GorjetaResposta.OK,
        var produtos: GorjetaLista<GorjetaProduto> =
            GorjetaLista(
                GorjetaResposta.OK,
                listOf(
                    GorjetaProduto("gorjeta_grande", "Grande", "R$ 25,00"),
                    GorjetaProduto("gorjeta_pequena", "Pequena", "R$ 5,00"),
                    GorjetaProduto("gorjeta_media", "Média", "R$ 10,00"),
                ),
            ),
        var emAberto: List<GorjetaCompra> = emptyList(),
        var respostaDaCompra: GorjetaResposta = GorjetaResposta.OK,
    ) : GorjetaLoja {
        val consumidas = mutableListOf<String>()
        val compradas = mutableListOf<String>()
        var ouvinte: (GorjetaResposta, List<GorjetaCompra>) -> Unit = { _, _ -> }
        var encerrada = false

        override suspend fun conectar() = conexao

        override suspend fun produtos(ids: List<String>) = produtos

        override suspend fun comprasEmAberto() = GorjetaLista(GorjetaResposta.OK, emAberto)

        override fun comprar(activity: Activity?, produtoId: String): GorjetaResposta {
            compradas += produtoId
            return respostaDaCompra
        }

        override suspend fun consumir(token: String): GorjetaResposta {
            consumidas += token
            emAberto = emAberto.filterNot { it.token == token }
            return GorjetaResposta.OK
        }

        override fun aoAtualizar(ouvinte: (GorjetaResposta, List<GorjetaCompra>) -> Unit) {
            this.ouvinte = ouvinte
        }

        override fun encerrar() {
            encerrada = true
        }
    }

    private fun maquina(loja: LojaFalsa) =
        GorjetaMaquina(loja, CoroutineScope(Dispatchers.Unconfined))

    private fun pronto(m: GorjetaMaquina) = m.estado.value as GorjetaEstado.Pronto

    @Test
    fun `produtos na ordem pequena, media, grande com preco da Play`() = runBlocking {
        val m = maquina(LojaFalsa())
        m.iniciar()
        val estado = pronto(m)
        assertEquals(
            listOf("gorjeta_pequena", "gorjeta_media", "gorjeta_grande"),
            estado.produtos.map { it.id },
        )
        assertEquals("R$ 5,00", estado.produtos[0].preco)
    }

    @Test
    fun `sem produtos no Console fica em breve`() = runBlocking {
        val m = maquina(LojaFalsa(produtos = GorjetaLista(GorjetaResposta.OK)))
        m.iniciar()
        assertEquals(GorjetaEstado.EmBreve, m.estado.value)
    }

    @Test
    fun `Play indisponivel e sem rede`() = runBlocking {
        val semPlay = maquina(LojaFalsa(conexao = GorjetaResposta.INDISPONIVEL))
        semPlay.iniciar()
        assertEquals(GorjetaEstado.Indisponivel(semRede = false), semPlay.estado.value)

        val semRede = maquina(LojaFalsa(conexao = GorjetaResposta.SEM_REDE))
        semRede.iniciar()
        assertEquals(GorjetaEstado.Indisponivel(semRede = true), semRede.estado.value)

        val consultaFalhou = maquina(LojaFalsa(produtos = GorjetaLista(GorjetaResposta.SEM_REDE)))
        consultaFalhou.iniciar()
        assertEquals(GorjetaEstado.Indisponivel(semRede = true), consultaFalhou.estado.value)
    }

    @Test
    fun `ao iniciar consome concluidas que ficaram para tras, nao as pendentes`() = runBlocking {
        val loja =
            LojaFalsa(
                emAberto =
                    listOf(
                        GorjetaCompra("antiga", GorjetaCompraEstado.CONCLUIDA),
                        GorjetaCompra("esperando", GorjetaCompraEstado.PENDENTE),
                    )
            )
        maquina(loja).iniciar()
        assertEquals(listOf("antiga"), loja.consumidas)
    }

    @Test
    fun `compra concluida e consumida e agradece`() = runBlocking {
        val loja = LojaFalsa()
        val m = maquina(loja)
        m.iniciar()
        m.comprar(null, "gorjeta_media")
        assertTrue(pronto(m).comprando)
        loja.ouvinte(
            GorjetaResposta.OK,
            listOf(GorjetaCompra("t1", GorjetaCompraEstado.CONCLUIDA)),
        )
        assertEquals(listOf("t1"), loja.consumidas)
        assertEquals(GorjetaAviso.OBRIGADO, pronto(m).aviso)
        assertEquals(false, pronto(m).comprando)
    }

    @Test
    fun `pendente nao consome e avisa, e consome quando o app abre depois`() = runBlocking {
        val loja = LojaFalsa()
        val m = maquina(loja)
        m.iniciar()
        m.comprar(null, "gorjeta_pequena")
        loja.ouvinte(GorjetaResposta.OK, listOf(GorjetaCompra("p1", GorjetaCompraEstado.PENDENTE)))
        assertTrue(loja.consumidas.isEmpty())
        assertEquals(GorjetaAviso.PENDENTE, pronto(m).aviso)

        // Banco confirmou mais tarde; na próxima abertura do app
        loja.emAberto = listOf(GorjetaCompra("p1", GorjetaCompraEstado.CONCLUIDA))
        assertEquals(1, maquina(loja).consumirEmAberto())
        assertEquals(listOf("p1"), loja.consumidas)
    }

    @Test
    fun `cancelado e sem rede viram aviso`() = runBlocking {
        val loja = LojaFalsa()
        val m = maquina(loja)
        m.iniciar()
        m.comprar(null, "gorjeta_grande")
        loja.ouvinte(GorjetaResposta.CANCELADO, emptyList())
        assertEquals(GorjetaAviso.CANCELADO, pronto(m).aviso)
        loja.ouvinte(GorjetaResposta.SEM_REDE, emptyList())
        assertEquals(GorjetaAviso.SEM_REDE, pronto(m).aviso)
    }

    @Test
    fun `produto nao encontrado ao comprar mostra indisponivel`() = runBlocking {
        val loja = LojaFalsa(respostaDaCompra = GorjetaResposta.INDISPONIVEL)
        val m = maquina(loja)
        m.iniciar()
        m.comprar(null, "gorjeta_que_nao_existe")
        assertEquals(GorjetaAviso.INDISPONIVEL, pronto(m).aviso)
        assertEquals(false, pronto(m).comprando)
    }

    @Test
    fun `gorjeta anterior nao consumida e consumida e agradece`() = runBlocking {
        val loja = LojaFalsa(respostaDaCompra = GorjetaResposta.JA_TEM)
        val m = maquina(loja)
        m.iniciar()
        loja.emAberto = listOf(GorjetaCompra("velha", GorjetaCompraEstado.CONCLUIDA))
        m.comprar(null, "gorjeta_media")
        assertEquals(listOf("velha"), loja.consumidas)
        assertEquals(GorjetaAviso.OBRIGADO, pronto(m).aviso)
    }

    @Test
    fun `nao abre duas compras ao mesmo tempo`() = runBlocking {
        val loja = LojaFalsa()
        val m = maquina(loja)
        m.iniciar()
        m.comprar(null, "gorjeta_media")
        m.comprar(null, "gorjeta_grande")
        assertEquals(listOf("gorjeta_media"), loja.compradas)
    }

    @Test
    fun `encerrar fecha a conexao`() {
        val loja = LojaFalsa()
        maquina(loja).encerrar()
        assertTrue(loja.encerrada)
    }
}
