package dev.jdtech.jellyfin.cgflix.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// CGFLIX: categorias pela biblioteca, nunca pelo tipo do item (Séries e Animes guardam "Series").
class CgflixCategoriesTest {
    private val filmes = CgflixLibraryInfo("lib-filmes", "Filmes", "movies")
    private val series = CgflixLibraryInfo("lib-series", "Séries", "tvshows")
    private val animes = CgflixLibraryInfo("lib-animes", "Animes", "tvshows")

    /** Servidor falso: cada item sabe de qual biblioteca é; a consulta filtra por ParentId. */
    private data class FakeItem(val name: String, val type: String, val libraryId: String)

    private val fakeServer =
        listOf(
            FakeItem("Ainda Estou Aqui", "Movie", filmes.id),
            FakeItem("Central do Brasil", "Movie", filmes.id),
            FakeItem("Sintonia", "Series", series.id),
            FakeItem("Cidade Invisível", "Series", series.id),
            FakeItem("One Piece", "Series", animes.id),
            FakeItem("Naruto", "Series", animes.id),
        )

    private fun queryByParentId(parentId: String?) = fakeServer.filter { it.libraryId == parentId }

    @Test
    fun `acha a biblioteca de cada categoria pelo nome`() {
        val map = cgflixResolveCategories(listOf(animes, filmes, series))
        assertEquals(filmes, map[CgflixCategory.FILMES])
        assertEquals(series, map[CgflixCategory.SERIES])
        assertEquals(animes, map[CgflixCategory.ANIMES])
        // a ordem dos chips é sempre Filmes, Séries, Animes
        assertEquals(CgflixCategory.entries.toList(), map.keys.toList())
    }

    @Test
    fun `nome sem acento e em outra caixa tambem vale`() {
        val map =
            cgflixResolveCategories(
                listOf(
                    CgflixLibraryInfo("a", "ANIME", "tvshows"),
                    CgflixLibraryInfo("s", "series", "TvShows"),
                )
            )
        assertEquals("s", map[CgflixCategory.SERIES]?.id)
        assertEquals("a", map[CgflixCategory.ANIMES]?.id)
    }

    @Test
    fun `sem biblioteca Animes o chip some`() {
        val map = cgflixResolveCategories(listOf(filmes, series))
        assertNull(map[CgflixCategory.ANIMES])
        assertEquals(2, map.size)
        assertFalse(cgflixHomeRows(map).any { it.category == CgflixCategory.ANIMES })
    }

    @Test
    fun `Animes nunca vira Series mesmo vindo primeiro`() {
        val map =
            cgflixResolveCategories(listOf(animes, CgflixLibraryInfo("x", "Novelas", "tvshows")))
        assertEquals("x", map[CgflixCategory.SERIES]?.id)
        assertEquals(animes, map[CgflixCategory.ANIMES])
    }

    @Test
    fun `cada linha de categoria so mostra o seu`() {
        val map = cgflixResolveCategories(listOf(filmes, series, animes))
        val rows = cgflixHomeRows(map).filter { it.kind == CgflixRowSpec.Kind.RECENT }
        assertEquals(3, rows.size)
        for (row in rows) {
            val items = queryByParentId(row.libraryId)
            assertTrue(items.isNotEmpty())
            assertTrue(
                "${row.title} mostrou item de outra biblioteca",
                items.all { it.libraryId == map[row.category]!!.id },
            )
        }
        val animeRow = rows.first { it.category == CgflixCategory.ANIMES }
        assertEquals(
            listOf("One Piece", "Naruto"),
            queryByParentId(animeRow.libraryId).map { it.name },
        )
        val seriesRow = rows.first { it.category == CgflixCategory.SERIES }
        assertEquals(
            listOf("Sintonia", "Cidade Invisível"),
            queryByParentId(seriesRow.libraryId).map { it.name },
        )
    }

    @Test
    fun `linhas da categoria presas a biblioteca`() {
        val rows = cgflixCategoryRows(CgflixCategory.ANIMES, animes)
        assertTrue(rows.all { it.libraryId == animes.id && it.category == CgflixCategory.ANIMES })
    }

    @Test
    fun `ordem da Inicio sem banner nem novos episodios`() {
        val rows = cgflixHomeRows(cgflixResolveCategories(listOf(filmes, series, animes)))
        assertEquals(
            listOf(
                "Continuar assistindo",
                "Em alta no Brasil",
                "Filmes recentes",
                "Séries recentes",
                "Animes recentes",
            ),
            rows.map { it.title },
        )
    }

    @Test
    fun `nome de biblioteca do emalta vira categoria`() {
        assertEquals(CgflixCategory.FILMES, cgflixCategoryOfLibraryName("filmes"))
        assertEquals(CgflixCategory.SERIES, cgflixCategoryOfLibraryName("Séries"))
        assertEquals(CgflixCategory.ANIMES, cgflixCategoryOfLibraryName("animes"))
        assertNull(cgflixCategoryOfLibraryName("musica"))
    }
}
