package dev.jdtech.jellyfin.cgflix.apoio

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** CGFLIX (sabor `play`): [GorjetaLoja] com o `BillingClient` do Google Play Billing. */
class PlayGorjetaLoja(context: Context) : GorjetaLoja {
    private var ouvinte: (GorjetaResposta, List<GorjetaCompra>) -> Unit = { _, _ -> }
    private val detalhes = mutableMapOf<String, ProductDetails>()

    private val cliente =
        BillingClient.newBuilder(context.applicationContext)
            .setListener { resultado, compras ->
                ouvinte(resposta(resultado), compras.orEmpty().map(::compra))
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .enableAutoServiceReconnection()
            .build()

    override fun aoAtualizar(ouvinte: (GorjetaResposta, List<GorjetaCompra>) -> Unit) {
        this.ouvinte = ouvinte
    }

    override suspend fun conectar(): GorjetaResposta = suspendCancellableCoroutine { cont ->
        cliente.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(resultado: BillingResult) {
                    if (cont.isActive) cont.resume(resposta(resultado))
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(GorjetaResposta.SEM_REDE)
                }
            }
        )
    }

    override suspend fun produtos(ids: List<String>): GorjetaLista<GorjetaProduto> {
        val params =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    ids.map {
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(it)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    }
                )
                .build()
        return suspendCancellableCoroutine { cont ->
            cliente.queryProductDetailsAsync(params) { resultado, consulta ->
                val lista = consulta.productDetailsList
                lista.forEach { detalhes[it.productId] = it }
                val produtos = lista.mapNotNull { produto ->
                    val oferta = produto.oneTimePurchaseOfferDetails ?: return@mapNotNull null
                    GorjetaProduto(produto.productId, produto.name, oferta.formattedPrice)
                }
                if (cont.isActive) cont.resume(GorjetaLista(resposta(resultado), produtos))
            }
        }
    }

    override suspend fun comprasEmAberto(): GorjetaLista<GorjetaCompra> {
        val params =
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        return suspendCancellableCoroutine { cont ->
            cliente.queryPurchasesAsync(params) { resultado, compras ->
                if (cont.isActive) {
                    cont.resume(GorjetaLista(resposta(resultado), compras.map(::compra)))
                }
            }
        }
    }

    override fun comprar(activity: Activity?, produtoId: String): GorjetaResposta {
        val produto = detalhes[produtoId] ?: return GorjetaResposta.INDISPONIVEL
        if (activity == null) return GorjetaResposta.ERRO
        val params =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(produto)
                            .build()
                    )
                )
                .build()
        return resposta(cliente.launchBillingFlow(activity, params))
    }

    override suspend fun consumir(token: String): GorjetaResposta =
        suspendCancellableCoroutine { cont ->
            val params = ConsumeParams.newBuilder().setPurchaseToken(token).build()
            cliente.consumeAsync(params) { resultado, _ ->
                if (cont.isActive) cont.resume(resposta(resultado))
            }
        }

    override fun encerrar() = cliente.endConnection()

    private fun compra(compra: Purchase) =
        GorjetaCompra(
            token = compra.purchaseToken,
            estado =
                when (compra.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> GorjetaCompraEstado.CONCLUIDA
                    Purchase.PurchaseState.PENDING -> GorjetaCompraEstado.PENDENTE
                    else -> GorjetaCompraEstado.OUTRO
                },
        )

    private fun resposta(resultado: BillingResult) =
        when (resultado.responseCode) {
            BillingResponseCode.OK -> GorjetaResposta.OK
            BillingResponseCode.USER_CANCELED -> GorjetaResposta.CANCELADO
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.NETWORK_ERROR -> GorjetaResposta.SEM_REDE
            BillingResponseCode.ITEM_ALREADY_OWNED -> GorjetaResposta.JA_TEM
            BillingResponseCode.BILLING_UNAVAILABLE,
            BillingResponseCode.ITEM_UNAVAILABLE,
            BillingResponseCode.FEATURE_NOT_SUPPORTED -> GorjetaResposta.INDISPONIVEL
            else -> GorjetaResposta.ERRO
        }
}
