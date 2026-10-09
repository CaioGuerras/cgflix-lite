package dev.jdtech.jellyfin.cgflix.logic

import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

// CGFLIX (Etapa 1B): pedidos (Seerr) embutidos na busca, sem tela de login, igual ao site e ao app
// completo (lib/cgflix/requests/cgflix_seerr.dart da 1E):
//   1. acha o Seerr ao lado do Jellyfin (netflix.docaio.com.br → pedidos.docaio.com.br), sem
//      endereço pré-preenchido no código;
//   2. POST {seerr}/api/v1/auth/jellyfin/quickconnect/initiate → {code, secret};
//   3. aprova o código no Jellyfin com o token da própria pessoa (POST /QuickConnect/Authorize);
//   4. POST .../quickconnect/authenticate com o secret → cookie connect.sid.
// O cookie fica no armazenamento seguro (CgflixSecureStore) e é renovado sozinho em 401/403.
// Se algo falhar, a busca só esconde "Disponível para pedir". Nunca formulário, nunca WebView.
// O cookie, o code e o secret nunca vão para o log.

// ---------------------------------------------------------------------------
// Modelos

enum class CgflixRequestState {
    /** Ninguém pediu ainda: botão "Pedir". */
    REQUESTABLE,
    /** Já pedido, esperando (selo "Pedido"). */
    REQUESTED,
    /** Aprovado e baixando (selo "Baixando"). */
    DOWNLOADING,
}

data class CgflixRequestable(
    val tmdbId: Int,
    val isMovie: Boolean,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
    val state: CgflixRequestState = CgflixRequestState.REQUESTABLE,
)

enum class CgflixMyRequestStatus(val label: String) {
    WAITING_APPROVAL("Aguardando aprovação"),
    APPROVED("Aprovado"),
    DOWNLOADING("Baixando"),
    PARTIALLY_AVAILABLE("Chegou em parte"),
    AVAILABLE("Disponível"),
    DECLINED("Recusado"),
    FAILED("Falhou"),
}

data class CgflixMyRequest(
    val id: Int,
    val tmdbId: Int,
    val isMovie: Boolean,
    val status: CgflixMyRequestStatus,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
)

/** Os pedidos não estão disponíveis agora (Seerr fora do ar, Quick Connect desligado...). */
class CgflixRequestsUnavailable(reason: String) : Exception(reason)

/** O Seerr recusou o pedido. [message] já em português, para mostrar à pessoa. */
class CgflixRequestRejected(override val message: String) : Exception(message)

// ---------------------------------------------------------------------------
// Regras sem rede

/**
 * `mediaInfo.status` do Seerr → situação na busca; `null` = já é nosso (ou bloqueado): não mostrar.
 * ausente/1 = pedir; 2 = "Pedido"; 3 = "Baixando"; 4/5 = disponível (já aparece como nosso); 6 =
 * bloqueado; 7 = apagado do servidor (dá para pedir de novo).
 */
fun cgflixRequestStateFor(mediaStatus: Int?): CgflixRequestState? =
    when (mediaStatus) {
        null,
        1,
        7 -> CgflixRequestState.REQUESTABLE
        2 -> CgflixRequestState.REQUESTED
        3 -> CgflixRequestState.DOWNLOADING
        else -> null
    }

fun cgflixMyRequestStatusFor(requestStatus: Int?, mediaStatus: Int?): CgflixMyRequestStatus =
    when (requestStatus) {
        3 -> CgflixMyRequestStatus.DECLINED
        4 -> CgflixMyRequestStatus.FAILED
        1 -> CgflixMyRequestStatus.WAITING_APPROVAL
        else ->
            when (mediaStatus) {
                5 -> CgflixMyRequestStatus.AVAILABLE
                4 -> CgflixMyRequestStatus.PARTIALLY_AVAILABLE
                3 -> CgflixMyRequestStatus.DOWNLOADING
                else ->
                    if (requestStatus == 5) CgflixMyRequestStatus.AVAILABLE
                    else CgflixMyRequestStatus.APPROVED
            }
    }

/**
 * Onde procurar o Seerr a partir do endereço do Jellyfin: troca o primeiro nome do domínio por
 * nomes comuns ("pedidos" primeiro, o do CGFLIX): `https://netflix.docaio.com.br` →
 * `https://pedidos.docaio.com.br`. Sem domínio com 3 partes (IP, "localhost"), tenta só a porta
 * padrão do Seerr no mesmo host.
 */
fun cgflixSeerrCandidates(jellyfinBaseUrl: String): List<String> {
    val uri = runCatching { URI(jellyfinBaseUrl.trim()) }.getOrNull() ?: return emptyList()
    val host = uri.host?.takeIf { it.isNotEmpty() } ?: return emptyList()
    val scheme = uri.scheme ?: "https"
    val isIp = host.all { it.isDigit() || it == '.' } || host.contains(':')
    val labels = host.split('.')
    if (isIp || labels.size < 3) return listOf("$scheme://$host:5055")
    val parent = labels.drop(1).joinToString(".")
    return listOf("pedidos", "seerr", "jellyseerr", "requests", "overseerr").map {
        "https://$it.$parent"
    }
}

/** Mesma regra do site: só caminho simples de imagem do TMDB. */
private val posterPathRegex = Regex("^/[A-Za-z0-9_-]+\\.(jpg|jpeg|png|webp)$")

/**
 * Pôster pelo proxy do próprio Seerr (o aparelho não fala com o TMDB). Caminho fora da regra →
 * `null` (cartão sem pôster).
 */
fun cgflixSeerrPoster(seerrBaseUrl: String, posterPath: String?): String? =
    if (posterPath != null && posterPathRegex.matches(posterPath)) {
        "${seerrBaseUrl.trimEnd('/')}/imageproxy/tmdb/t/p/w300_and_h450_face$posterPath"
    } else {
        null
    }

private fun yearOf(date: String?): Int? = date?.takeIf { it.length >= 4 }?.take(4)?.toIntOrNull()

// ---------------------------------------------------------------------------
// Transporte e armazenamento (trocáveis nos testes)

data class CgflixHttpRequest(
    val method: String,
    val url: String,
    val jsonBody: String? = null,
    val cookie: String? = null,
)

data class CgflixHttpResponse(
    val code: Int,
    val body: String,
    val setCookies: List<String> = emptyList(),
)

fun interface CgflixHttp {
    /** Lança [IOException] se não houver resposta (rede, DNS, tempo esgotado). */
    suspend fun send(request: CgflixHttpRequest): CgflixHttpResponse
}

interface CgflixSessionStore {
    fun read(key: String): String?

    fun write(key: String, value: String)

    fun delete(key: String)
}

// ---------------------------------------------------------------------------
// Cliente

class CgflixSeerrClient(
    /** Endereço em uso do Jellyfin (de onde o endereço do Seerr é tirado). */
    private val jellyfinBaseUrl: String,
    /** Servidor + usuário: separa a sessão de cada conta no aparelho. */
    private val accountKey: String,
    private val http: CgflixHttp,
    private val store: CgflixSessionStore,
    /** Aprova o código do Quick Connect no Jellyfin com o token da pessoa. */
    private val authorizeQuickConnect: suspend (code: String) -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
    private val retryDelayMs: Long = 1_000,
) {
    private data class Session(val baseUrl: String, val cookie: String, val userId: Int)

    private val json = Json { ignoreUnknownKeys = true }
    private val signInMutex = Mutex()
    @Volatile private var session: Session? = null
    @Volatile private var failedAt: Long? = null

    private val storeKey
        get() = "seerr_session:$accountKey"

    /** Endereço do Seerr em uso (para montar o pôster), depois da primeira chamada. */
    val seerrBaseUrl: String?
        get() = session?.baseUrl

    suspend fun search(query: String): List<CgflixRequestable> {
        val q = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")
        val data = getJson("/search?query=$q&page=1&language=pt-BR") as? JsonObject
        val base = currentSession().baseUrl
        val results = data?.get("results") as? JsonArray ?: return emptyList()
        return results.mapNotNull { raw ->
            val item = raw as? JsonObject ?: return@mapNotNull null
            val mediaType = item.str("mediaType")
            if (mediaType != "movie" && mediaType != "tv") return@mapNotNull null
            val id = item.int("id") ?: return@mapNotNull null
            val status = (item["mediaInfo"] as? JsonObject)?.int("status")
            val state = cgflixRequestStateFor(status) ?: return@mapNotNull null
            CgflixRequestable(
                tmdbId = id,
                isMovie = mediaType == "movie",
                title =
                    item.str("title")
                        ?: item.str("name")
                        ?: item.str("originalTitle")
                        ?: item.str("originalName")
                        ?: "",
                year = yearOf(item.str("releaseDate") ?: item.str("firstAirDate")),
                posterUrl = cgflixSeerrPoster(base, item.str("posterPath")),
                state = state,
            )
        }
    }

    /** Filme: pede direto. Série: pede todas as temporadas (o Lite não tem tela de escolha). */
    suspend fun request(item: CgflixRequestable) {
        val body = buildJsonObject {
            put("mediaType", if (item.isMovie) "movie" else "tv")
            put("mediaId", item.tmdbId)
            if (!item.isMovie) put("seasons", "all")
        }
        val res = send("POST", "/request", body.toString())
        if (res.code < 400) return
        val message = runCatching {
            (json.parseToJsonElement(res.body) as? JsonObject)?.str("message")
        }
            .getOrNull()
        throw CgflixRequestRejected(
            when {
                res.code == 403 -> "Sua conta não tem permissão para pedir. Fale com o Caio."
                res.code == 409 -> "Esse título já foi pedido."
                message.orEmpty().lowercase().contains("quota") ->
                    "Você chegou ao limite de pedidos por agora."
                else -> "Não deu para fazer o pedido agora. Tente de novo mais tarde."
            }
        )
    }

    /** Pedidos da pessoa, mais novos primeiro, já com nome e pôster. */
    suspend fun myRequests(): List<CgflixMyRequest> {
        val current = currentSession()
        val data = getJson("/user/${current.userId}/requests?take=50&skip=0") as? JsonObject
        val results = data?.get("results") as? JsonArray ?: return emptyList()
        data class Raw(
            val id: Int,
            val tmdbId: Int,
            val isMovie: Boolean,
            val status: CgflixMyRequestStatus,
            val createdAt: String,
        )
        val raws =
            results
                .mapNotNull { element ->
                    val raw = element as? JsonObject ?: return@mapNotNull null
                    val media = raw["media"] as? JsonObject ?: return@mapNotNull null
                    val id = raw.int("id") ?: return@mapNotNull null
                    val tmdbId = media.int("tmdbId") ?: return@mapNotNull null
                    Raw(
                        id = id,
                        tmdbId = tmdbId,
                        isMovie = media.str("mediaType") == "movie" || raw.str("type") == "movie",
                        status = cgflixMyRequestStatusFor(raw.int("status"), media.int("status")),
                        createdAt = raw.str("createdAt").orEmpty(),
                    )
                }
                .sortedByDescending { it.createdAt }
        // O Seerr não manda o nome no pedido: busca cada título (em paralelo; falha vira só o
        // número)
        return coroutineScope {
            raws
                .map { r ->
                    async {
                        val info = runCatching { titleInfo(r.tmdbId, r.isMovie) }.getOrNull()
                        CgflixMyRequest(
                            id = r.id,
                            tmdbId = r.tmdbId,
                            isMovie = r.isMovie,
                            status = r.status,
                            title =
                                info?.first?.takeIf { it.isNotBlank() } ?: "Título nº ${r.tmdbId}",
                            year = info?.second,
                            posterUrl = info?.third,
                        )
                    }
                }
                .awaitAll()
        }
    }

    private suspend fun titleInfo(tmdbId: Int, isMovie: Boolean): Triple<String, Int?, String?> {
        val path = if (isMovie) "/movie/$tmdbId" else "/tv/$tmdbId"
        val data = getJson("$path?language=pt-BR") as? JsonObject ?: return Triple("", null, null)
        return Triple(
            data.str("title") ?: data.str("name") ?: "",
            yearOf(data.str("releaseDate") ?: data.str("firstAirDate")),
            cgflixSeerrPoster(currentSession().baseUrl, data.str("posterPath")),
        )
    }

    // --- sessão -----------------------------------------------------------------------------

    private suspend fun currentSession(): Session {
        session?.let {
            return it
        }
        return signInMutex.withLock {
            session?.let {
                return@withLock it
            }
            decode(store.read(storeKey))?.let {
                session = it
                return@withLock it
            }
            val failed = failedAt
            if (failed != null && now() - failed < RETRY_AFTER_MS) {
                throw CgflixRequestsUnavailable("falhou há pouco; tenta de novo daqui a pouco")
            }
            signIn(preferredUrl = null)
        }
    }

    /** Esquece o cookie vencido (401/403) e entra de novo. */
    private suspend fun renew(stale: Session): Session = signInMutex.withLock {
        val current = session
        if (current != null && current != stale) return@withLock current
        session = null
        store.delete(storeKey)
        signIn(preferredUrl = stale.baseUrl)
    }

    private suspend fun signIn(preferredUrl: String?): Session =
        try {
            signInOnce(preferredUrl).also { failedAt = null }
        } catch (e: CgflixRequestsUnavailable) {
            failedAt = now()
            throw e
        }

    private suspend fun signInOnce(preferredUrl: String?): Session {
        val baseUrl = discover(preferredUrl)
        try {
            val initiate =
                http.send(
                    CgflixHttpRequest(
                        "POST",
                        "$baseUrl$API/auth/jellyfin/quickconnect/initiate",
                        "{}",
                    )
                )
            if (initiate.code >= 400) {
                throw CgflixRequestsUnavailable(
                    "Quick Connect indisponível no Seerr (${initiate.code})"
                )
            }
            val started = json.parseToJsonElement(initiate.body) as? JsonObject
            val code = started?.str("code")
            val secret = started?.str("secret")
            if (code.isNullOrEmpty() || secret.isNullOrEmpty()) {
                throw CgflixRequestsUnavailable("Quick Connect sem código")
            }
            authorizeQuickConnect(code)
            val body = buildJsonObject { put("secret", secret) }.toString()
            var attempt = 0
            while (true) {
                val res =
                    http.send(
                        CgflixHttpRequest(
                            "POST",
                            "$baseUrl$API/auth/jellyfin/quickconnect/authenticate",
                            body,
                        )
                    )
                val cookie = res.setCookies.firstNotNullOfOrNull { sessionCookieOf(it) }
                if (res.code < 400 && cookie != null) {
                    val userId =
                        runCatching {
                            (json.parseToJsonElement(res.body) as? JsonObject)?.int("id")
                        }
                            .getOrNull() ?: fetchUserId(baseUrl, cookie)
                    val fresh = Session(baseUrl, cookie, userId)
                    store.write(storeKey, encode(fresh))
                    session = fresh
                    return fresh
                }
                // A aprovação no Jellyfin pode levar um instante para chegar ao Seerr
                if (++attempt >= AUTH_ATTEMPTS) {
                    throw CgflixRequestsUnavailable("Quick Connect não foi aceito (${res.code})")
                }
                delay(retryDelayMs)
            }
        } catch (e: CgflixRequestsUnavailable) {
            throw e
        } catch (e: Exception) {
            // Sem detalhes (code/secret/cookie) no log nem na mensagem
            throw CgflixRequestsUnavailable("entrada automática falhou: ${e.javaClass.simpleName}")
        }
    }

    private suspend fun fetchUserId(baseUrl: String, cookie: String): Int {
        val res = http.send(CgflixHttpRequest("GET", "$baseUrl$API/auth/me", cookie = cookie))
        return (json.parseToJsonElement(res.body) as? JsonObject)?.int("id")
            ?: throw CgflixRequestsUnavailable("Seerr não disse quem é a pessoa")
    }

    /** Primeiro endereço (na ordem de preferência) que responde como Seerr. Testa todos juntos. */
    private suspend fun discover(preferred: String?): String {
        val candidates =
            (listOfNotNull(preferred) + cgflixSeerrCandidates(jellyfinBaseUrl)).distinct()
        if (candidates.isEmpty()) throw CgflixRequestsUnavailable("sem endereço do servidor")
        val answered = coroutineScope {
            candidates
                .map { candidate ->
                    async {
                        try {
                            val res =
                                http.send(
                                    CgflixHttpRequest("GET", "$candidate$API/settings/public")
                                )
                            res.code == 200 && res.body.trimStart().startsWith("{")
                        } catch (_: Exception) {
                            false
                        }
                    }
                }
                .awaitAll()
        }
        val index = answered.indexOfFirst { it }
        if (index < 0) throw CgflixRequestsUnavailable("Seerr não encontrado ao lado do servidor")
        return candidates[index]
    }

    /** Chamada autenticada; em 401/403 renova a sessão e tenta mais uma vez. */
    private suspend fun send(
        method: String,
        path: String,
        body: String? = null,
    ): CgflixHttpResponse {
        var current = currentSession()
        var attempt = 0
        while (true) {
            val res =
                try {
                    http.send(
                        CgflixHttpRequest(
                            method,
                            "${current.baseUrl}$API$path",
                            body,
                            current.cookie,
                        )
                    )
                } catch (e: IOException) {
                    throw CgflixRequestsUnavailable("Seerr fora do ar: ${e.javaClass.simpleName}")
                }
            if ((res.code == 401 || res.code == 403) && attempt == 0) {
                attempt++
                current = renew(current)
                continue
            }
            return res
        }
    }

    private suspend fun getJson(path: String): JsonElement? {
        val res = send("GET", path)
        if (res.code >= 400) throw CgflixRequestsUnavailable("Seerr respondeu ${res.code}")
        return runCatching { json.parseToJsonElement(res.body) }.getOrNull()
    }

    private fun encode(s: Session): String = buildJsonObject {
        put("url", s.baseUrl)
        put("cookie", s.cookie)
        put("user", s.userId)
    }
        .toString()

    private fun decode(raw: String?): Session? {
        if (raw.isNullOrEmpty()) return null
        return runCatching {
            val obj = json.parseToJsonElement(raw) as JsonObject
            Session(obj.str("url")!!, obj.str("cookie")!!, obj.int("user")!!)
        }
            .getOrNull()
    }

    companion object {
        private const val API = "/api/v1"
        private const val AUTH_ATTEMPTS = 3
        private const val RETRY_AFTER_MS = 2L * 60L * 1000L

        /** `connect.sid=...; Path=/; HttpOnly` → `connect.sid=...`. */
        fun sessionCookieOf(setCookie: String): String? =
            setCookie.substringBefore(';').trim().takeIf {
                it.startsWith("connect.sid=") && it.length > "connect.sid=".length
            }
    }
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
