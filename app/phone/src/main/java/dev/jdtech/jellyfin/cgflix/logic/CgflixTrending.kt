package dev.jdtech.jellyfin.cgflix.logic

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// CGFLIX (Etapa 1B): "Em alta no Brasil". O servidor publica `<servidor>/cgflix/emalta.json`:
// {"titulo","itens":[{"id","nome","tipo","biblioteca"}],
//  "porBiblioteca":{"filmes":[...],"series":[...],"animes":[...]}}, já na ordem do ranking.
// Mesma leitura do app completo (lib/cgflix/home/cgflix_home_logic.dart da 1E).

const val CGFLIX_TRENDING_TITLE = "Em alta no Brasil"
const val CGFLIX_TRENDING_PATH = "/cgflix/emalta.json"

/** O "Em alta" vale 1 h no aparelho. */
const val CGFLIX_TRENDING_TTL_MS = 60L * 60L * 1000L

/** Mínimo de títulos para a linha "Em alta" de uma categoria aparecer. */
const val CGFLIX_TRENDING_MIN_IN_CATEGORY = 3

data class CgflixTrendingEntry(
    val id: String,
    val name: String? = null,
    val type: String? = null,
    /** "filmes", "series" ou "animes", quando o arquivo diz. */
    val library: String? = null,
)

data class CgflixTrending(
    val title: String,
    /** Ranking geral (Início), na ordem. */
    val entries: List<CgflixTrendingEntry>,
    /** Ranking de cada categoria (`porBiblioteca`); nulo se o arquivo não tiver o campo. */
    val byCategory: Map<CgflixCategory, List<CgflixTrendingEntry>>? = null,
) {
    /** Ids da Início (`itens`), na ordem. */
    val homeIds: List<String>
        get() = entries.map { it.id }

    /**
     * Ids do "Em alta" de uma categoria, na ordem; `null` = esconder a linha.
     * 1. `porBiblioteca` (o certo); 2. sem ele, os `itens` cuja `biblioteca` é a da categoria;
     * 3. sem nenhum dos dois, Filmes ainda separa pelo tipo (filme é sempre "Movie"), mas Séries e
     *    Animes não têm como (as duas guardam "Series"): a linha some.
     * Com menos de [CGFLIX_TRENDING_MIN_IN_CATEGORY] títulos a linha também some.
     */
    fun idsFor(category: CgflixCategory): List<String>? {
        val picked =
            when {
                byCategory != null -> byCategory[category].orEmpty()
                entries.any { it.library != null } ->
                    entries.filter { cgflixCategoryOfLibraryName(it.library) == category }
                category == CgflixCategory.FILMES ->
                    entries.filter { it.type == null || it.type == "Movie" }
                else -> return null
            }
        val ids = picked.take(10).map { it.id }
        return if (ids.size < CGFLIX_TRENDING_MIN_IN_CATEGORY) null else ids
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Lê o emalta.json. Tolerante: ignora itens sem id e repetidos; `null` se nada serve. */
        fun parse(text: String): CgflixTrending? {
            val parsed =
                try {
                    json.parseToJsonElement(text)
                } catch (_: Exception) {
                    return null
                }
            val root = parsed as? JsonObject ?: return null
            val entries = parseEntries(root["itens"])
            val byCategory =
                (root["porBiblioteca"] as? JsonObject)?.let { raw ->
                    buildMap {
                        for ((key, value) in raw) {
                            cgflixCategoryOfLibraryName(key)?.let { put(it, parseEntries(value)) }
                        }
                    }
                }
            if (entries.isEmpty() && byCategory.orEmpty().values.all { it.isEmpty() }) return null
            val title = root.string("titulo")?.trim()?.takeIf { it.isNotEmpty() }
            return CgflixTrending(title ?: CGFLIX_TRENDING_TITLE, entries, byCategory)
        }

        private fun parseEntries(raw: JsonElement?): List<CgflixTrendingEntry> {
            val array = raw as? JsonArray ?: return emptyList()
            val seen = HashSet<String>()
            return array.mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                val id = (item["id"] as? JsonPrimitive)?.content?.trim().orEmpty()
                if (id.isEmpty() || id == "null" || !seen.add(id)) return@mapNotNull null
                CgflixTrendingEntry(
                    id = id,
                    name = item.string("nome"),
                    type = item.string("tipo"),
                    library = item.string("biblioteca")?.trim()?.takeIf { it.isNotEmpty() },
                )
            }
        }

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}

/**
 * Reordena [items] pela ordem de [ids] (o `/Items?Ids=` do Jellyfin não garante ordem). Itens
 * que o servidor não devolveu (apagados, sem permissão) somem.
 */
fun <T> cgflixOrderByIds(items: List<T>, ids: List<String>, idOf: (T) -> String): List<T> {
    val byId = items.associateBy { cgflixCompactId(idOf(it)) }
    return ids.mapNotNull { byId[cgflixCompactId(it)] }
}

/** Ids do Jellyfin chegam com ou sem hífens e em qualquer caixa. */
fun cgflixCompactId(id: String): String = id.replace("-", "").lowercase()

/** Cache simples com prazo (o "Em alta" vale 1 h). [now] é trocável nos testes. */
class CgflixTtlCache<T>(private val ttlMs: Long, private val now: () -> Long = System::currentTimeMillis) {
    private var value: T? = null
    private var storedAt = 0L

    @Synchronized fun get(): T? = value?.takeIf { now() - storedAt < ttlMs }

    @Synchronized
    fun put(newValue: T, at: Long = now()) {
        value = newValue
        storedAt = at
    }

    @Synchronized
    fun clear() {
        value = null
    }
}
