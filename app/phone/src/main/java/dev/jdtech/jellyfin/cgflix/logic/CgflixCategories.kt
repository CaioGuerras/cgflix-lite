package dev.jdtech.jellyfin.cgflix.logic

import java.text.Normalizer

// CGFLIX (Etapa 1B): categorias Filmes · Séries · Animes. Regras sem Android, testadas em
// app/phone/src/test. Lição da 1E do app completo: Séries e Animes são as duas bibliotecas
// "tvshows" e guardam itens do mesmo tipo (Series), então a categoria vem SEMPRE da biblioteca
// (o Id dela, achado pelo nome), nunca do tipo do item.

enum class CgflixCategory(val label: String) {
    FILMES("Filmes"),
    SERIES("Séries"),
    ANIMES("Animes"),
}

/** O mínimo de uma biblioteca do Jellyfin que as regras precisam. */
data class CgflixLibraryInfo(val id: String, val name: String, val collectionType: String?)

/** Minúsculas e sem acento, para comparar nomes ("Séries" == "series"). */
fun cgflixNormalize(text: String): String =
    Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

private fun isAnimeName(name: String) =
    cgflixNormalize(name).let { it == "animes" || it == "anime" }

/**
 * Biblioteca de cada categoria, na ordem Filmes, Séries, Animes:
 * - Filmes: a biblioteca `movies` (de preferência a chamada "Filmes");
 * - Séries: a biblioteca `tvshows` chamada "Séries" (sem ela, a primeira `tvshows` que não é a de
 *   animes);
 * - Animes: a biblioteca `tvshows` chamada "Animes". Sem ela, a categoria (e o chip) some.
 */
fun cgflixResolveCategories(
    libraries: List<CgflixLibraryInfo>
): Map<CgflixCategory, CgflixLibraryInfo> {
    fun type(l: CgflixLibraryInfo) = l.collectionType?.lowercase()
    val movies = libraries.filter { type(it) == "movies" }
    val shows = libraries.filter { type(it) == "tvshows" }
    val result = linkedMapOf<CgflixCategory, CgflixLibraryInfo>()
    (movies.firstOrNull { cgflixNormalize(it.name) == "filmes" } ?: movies.firstOrNull())?.let {
        result[CgflixCategory.FILMES] = it
    }
    (shows.firstOrNull { cgflixNormalize(it.name) == "series" }
            ?: shows.firstOrNull { !isAnimeName(it.name) })
        ?.let { result[CgflixCategory.SERIES] = it }
    shows.firstOrNull { isAnimeName(it.name) }?.let { result[CgflixCategory.ANIMES] = it }
    return result
}

/** Categoria de um nome de biblioteca do emalta.json ("filmes", "Séries", "animes"...). */
fun cgflixCategoryOfLibraryName(raw: String?): CgflixCategory? {
    if (raw == null) return null
    val name = cgflixNormalize(raw)
    return when {
        name.startsWith("film") || name == "movies" || name == "movie" -> CgflixCategory.FILMES
        name.startsWith("anime") -> CgflixCategory.ANIMES
        name.startsWith("serie") || name == "shows" || name == "tvshows" -> CgflixCategory.SERIES
        else -> null
    }
}

/** Uma linha da Início ou de uma categoria. */
data class CgflixRowSpec(
    val key: String,
    val title: String,
    val kind: Kind,
    /** Biblioteca da linha (`ParentId` da consulta); nula nas linhas gerais. */
    val libraryId: String? = null,
    val category: CgflixCategory? = null,
) {
    enum class Kind {
        CONTINUE,
        TRENDING,
        RECENT,
    }
}

/**
 * Ordem da Início: Continuar assistindo → Em alta no Brasil → Filmes, Séries e Animes recentes
 * (cada um só da sua biblioteca; categoria sem biblioteca não tem linha). Sem banner de destaque
 * nem "Novos episódios" soltos.
 */
fun cgflixHomeRows(categories: Map<CgflixCategory, CgflixLibraryInfo>): List<CgflixRowSpec> =
    buildList {
        add(CgflixRowSpec("continuar", "Continuar assistindo", CgflixRowSpec.Kind.CONTINUE))
        add(CgflixRowSpec("emalta", CGFLIX_TRENDING_TITLE, CgflixRowSpec.Kind.TRENDING))
        for (category in CgflixCategory.entries) {
            val library = categories[category] ?: continue
            add(
                CgflixRowSpec(
                    key = "recentes-${category.name.lowercase()}",
                    title = "${category.label} recentes",
                    kind = CgflixRowSpec.Kind.RECENT,
                    libraryId = library.id,
                    category = category,
                )
            )
        }
    }

/**
 * Linhas de uma categoria (chip): Em alta da categoria e Adicionados recentemente, as duas presas à
 * biblioteca da categoria. A grade com tudo vem embaixo, também pelo `ParentId`.
 */
fun cgflixCategoryRows(category: CgflixCategory, library: CgflixLibraryInfo): List<CgflixRowSpec> =
    listOf(
        CgflixRowSpec(
            "emalta-${category.name.lowercase()}",
            "Em alta em ${category.label}",
            CgflixRowSpec.Kind.TRENDING,
            library.id,
            category,
        ),
        CgflixRowSpec(
            "recentes-${category.name.lowercase()}",
            "Adicionados recentemente",
            CgflixRowSpec.Kind.RECENT,
            library.id,
            category,
        ),
    )
