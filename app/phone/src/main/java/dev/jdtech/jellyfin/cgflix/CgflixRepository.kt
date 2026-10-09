package dev.jdtech.jellyfin.cgflix

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.cgflix.logic.CGFLIX_TRENDING_PATH
import dev.jdtech.jellyfin.cgflix.logic.CGFLIX_TRENDING_TITLE
import dev.jdtech.jellyfin.cgflix.logic.CGFLIX_TRENDING_TTL_MS
import dev.jdtech.jellyfin.cgflix.logic.CgflixCategory
import dev.jdtech.jellyfin.cgflix.logic.CgflixHttpRequest
import dev.jdtech.jellyfin.cgflix.logic.CgflixLibraryInfo
import dev.jdtech.jellyfin.cgflix.logic.CgflixOkHttp
import dev.jdtech.jellyfin.cgflix.logic.CgflixSeerrClient
import dev.jdtech.jellyfin.cgflix.logic.CgflixTrending
import dev.jdtech.jellyfin.cgflix.logic.CgflixTtlCache
import dev.jdtech.jellyfin.cgflix.logic.cgflixNormalize
import dev.jdtech.jellyfin.cgflix.logic.cgflixOrderByIds
import dev.jdtech.jellyfin.cgflix.logic.cgflixResolveCategories
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.toFindroidItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber

/**
 * CGFLIX (Etapa 1B): o que a Início, as categorias e os Pedidos pedem ao servidor, sem mexer no
 * repositório do Findroid. Usa o mesmo cliente do Jellyfin (token e endereço em uso) e converte
 * para os itens do Findroid, para reaproveitar cartões, página do título e player.
 */
@Singleton
class CgflixRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val jellyfinApi: JellyfinApi,
) {
    private val okHttp =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    private val http = CgflixOkHttp(okHttp)
    private val diskCache = context.getSharedPreferences("cgflix_cache", Context.MODE_PRIVATE)

    private val trendingCache =
        CgflixTtlCache<Pair<String, CgflixTrending?>>(CGFLIX_TRENDING_TTL_MS)
    @Volatile
    private var categoriesCache: Pair<String, Map<CgflixCategory, CgflixLibraryInfo>>? = null

    private val baseUrl: String
        get() = jellyfinApi.api.baseUrl.orEmpty().trimEnd('/')

    private val userId: UUID
        get() = jellyfinApi.userId ?: error("sem usuário")

    private val accountKey: String
        get() = "$baseUrl|${jellyfinApi.userId}"

    /** Bibliotecas de cada categoria (Filmes, Séries, Animes) da pessoa. */
    suspend fun categories(force: Boolean = false): Map<CgflixCategory, CgflixLibraryInfo> {
        val key = accountKey
        categoriesCache
            ?.takeIf { !force && it.first == key }
            ?.let {
                return it.second
            }
        val views =
            withContext(Dispatchers.IO) {
                jellyfinApi.viewsApi.getUserViews(userId).content.items.map {
                    CgflixLibraryInfo(
                        it.id.toString(),
                        it.name.orEmpty(),
                        it.collectionType?.serialName,
                    )
                }
            }
        return cgflixResolveCategories(views).also { categoriesCache = key to it }
    }

    /**
     * Em alta: emalta.json do servidor (1 h de cache na memória e no disco, para a Início abrir
     * rápido). `null` se o arquivo não existe ou não serve.
     */
    suspend fun trending(force: Boolean = false): CgflixTrending? {
        val key = baseUrl
        if (!force) {
            trendingCache
                .get()
                ?.takeIf { it.first == key }
                ?.let {
                    return it.second
                }
            val savedAt = diskCache.getLong("emalta_at:$key", 0L)
            val saved = diskCache.getString("emalta:$key", null)
            if (saved != null && System.currentTimeMillis() - savedAt < CGFLIX_TRENDING_TTL_MS) {
                val parsed = CgflixTrending.parse(saved)
                trendingCache.put(key to parsed, savedAt)
                return parsed
            }
        }
        val parsed =
            try {
                val res = http.send(CgflixHttpRequest("GET", "$key$CGFLIX_TRENDING_PATH"))
                if (res.code == 200) {
                    diskCache
                        .edit()
                        .putString("emalta:$key", res.body)
                        .putLong("emalta_at:$key", System.currentTimeMillis())
                        .apply()
                    CgflixTrending.parse(res.body)
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.i("CGFLIX: emalta.json indisponível (${e.javaClass.simpleName})")
                null
            }
        trendingCache.put(key to parsed)
        return parsed
    }

    /** Itens pelos Ids, na ordem dada (o servidor não garante ordem). */
    suspend fun itemsByIds(ids: List<String>, repository: JellyfinRepository): List<FindroidItem> {
        val uuids = ids.mapNotNull { runCatching { parseUuid(it) }.getOrNull() }
        if (uuids.isEmpty()) return emptyList()
        val items =
            withContext(Dispatchers.IO) {
                jellyfinApi.itemsApi
                    .getItems(userId, ids = uuids, limit = uuids.size)
                    .content
                    .items
                    .mapNotNull { it.toFindroidItem(repository) }
            }
        return cgflixOrderByIds(items, ids) { it.id.toString() }
    }

    /** Plano B do "Em alta": a coleção do Jellyfin chamada "Em alta no Brasil". */
    suspend fun trendingCollection(repository: JellyfinRepository): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            val collection =
                jellyfinApi.itemsApi
                    .getItems(
                        userId,
                        includeItemTypes = listOf(BaseItemKind.BOX_SET),
                        searchTerm = CGFLIX_TRENDING_TITLE,
                        recursive = true,
                        limit = 5,
                    )
                    .content
                    .items
                    .firstOrNull {
                        cgflixNormalize(it.name.orEmpty()) == cgflixNormalize(CGFLIX_TRENDING_TITLE)
                    } ?: return@withContext emptyList()
            jellyfinApi.itemsApi
                .getItems(
                    userId,
                    parentId = collection.id,
                    includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                    limit = 10,
                )
                .content
                .items
                .mapNotNull { it.toFindroidItem(repository) }
        }

    /**
     * Recentes de uma categoria, SEMPRE pelo `ParentId` da biblioteca. Filmes pela data em que
     * entraram; séries e animes pela chegada do último episódio (pôster da série, nunca episódio
     * solto).
     */
    suspend fun recent(
        category: CgflixCategory,
        libraryId: String,
        repository: JellyfinRepository,
        limit: Int = 16,
    ): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            val movies = category == CgflixCategory.FILMES
            jellyfinApi.itemsApi
                .getItems(
                    userId,
                    parentId = parseUuid(libraryId),
                    includeItemTypes =
                        listOf(if (movies) BaseItemKind.MOVIE else BaseItemKind.SERIES),
                    recursive = true,
                    sortBy =
                        listOf(
                            if (movies) ItemSortBy.DATE_CREATED
                            else ItemSortBy.DATE_LAST_CONTENT_ADDED
                        ),
                    sortOrder = listOf(SortOrder.DESCENDING),
                    limit = limit,
                )
                .content
                .items
                .mapNotNull { it.toFindroidItem(repository) }
        }

    /** Id do TVDB de uma série (para a música tema). */
    suspend fun tvdbIdOf(itemId: UUID): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                jellyfinApi.userLibraryApi
                    .getItem(itemId, userId)
                    .content
                    .providerIds
                    ?.entries
                    ?.firstOrNull { it.key.equals("Tvdb", ignoreCase = true) }
                    ?.value
            }
                .getOrNull()
                ?.takeIf { it.isNotBlank() && it.all(Char::isDigit) }
        }

    /** Endereço da música tema da série (`<servidor>/__tema/<tvdb>.mp3`). */
    fun themeMusicUrl(tvdbId: String): String = "$baseUrl/__tema/$tvdbId.mp3"

    // --- Pedidos (Seerr) --------------------------------------------------------------------

    private val secureStore by lazy { CgflixSecureStore(context) }
    @Volatile private var seerr: Pair<String, CgflixSeerrClient>? = null

    /** Cliente do Seerr da conta em uso (um por servidor + usuário). */
    fun seerr(): CgflixSeerrClient {
        val key = accountKey
        seerr
            ?.takeIf { it.first == key }
            ?.let {
                return it.second
            }
        val client =
            CgflixSeerrClient(
                jellyfinBaseUrl = baseUrl,
                accountKey = key,
                http = http,
                store = secureStore,
                authorizeQuickConnect = { code ->
                    withContext(Dispatchers.IO) {
                        jellyfinApi.quickConnectApi.authorizeQuickConnect(code, jellyfinApi.userId)
                    }
                },
            )
        seerr = key to client
        return client
    }

    private fun parseUuid(id: String): UUID =
        if (id.contains('-')) {
            UUID.fromString(id)
        } else {
            UUID.fromString(
                id.replaceFirst(
                    Regex("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})"),
                    "$1-$2-$3-$4-$5",
                )
            )
        }
}
