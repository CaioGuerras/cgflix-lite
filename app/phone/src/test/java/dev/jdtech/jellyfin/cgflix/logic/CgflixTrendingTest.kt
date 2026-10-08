package dev.jdtech.jellyfin.cgflix.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CgflixTrendingTest {
    private val full =
        """
        {"titulo":"Em alta no Brasil",
         "itens":[{"id":"m1","nome":"Filme 1","tipo":"Movie","biblioteca":"filmes"},
                  {"id":"s1","nome":"Série 1","tipo":"Series","biblioteca":"series"},
                  {"id":"a1","nome":"Anime 1","tipo":"Series","biblioteca":"animes"},
                  {"id":"m1"},{"nome":"sem id"}],
         "porBiblioteca":{"filmes":[{"id":"m1"},{"id":"m2"},{"id":"m3"}],
                          "series":[{"id":"s1"},{"id":"s2"}],
                          "animes":[{"id":"a1"},{"id":"a2"},{"id":"a3"},{"id":"a4"}]}}
        """

    @Test
    fun `Inicio usa itens na ordem sem repetidos nem sem id`() {
        val t = CgflixTrending.parse(full)!!
        assertEquals(listOf("m1", "s1", "a1"), t.homeIds)
        assertEquals("Em alta no Brasil", t.title)
    }

    @Test
    fun `categoria usa porBiblioteca e some com menos de 3`() {
        val t = CgflixTrending.parse(full)!!
        assertEquals(listOf("m1", "m2", "m3"), t.idsFor(CgflixCategory.FILMES))
        assertNull(t.idsFor(CgflixCategory.SERIES)) // só 2
        assertEquals(listOf("a1", "a2", "a3", "a4"), t.idsFor(CgflixCategory.ANIMES))
    }

    @Test
    fun `sem porBiblioteca filtra itens pela biblioteca`() {
        val json =
            """{"itens":[{"id":"a1","biblioteca":"animes"},{"id":"s1","biblioteca":"series"},
               {"id":"a2","biblioteca":"Animes"},{"id":"a3","biblioteca":"animes"}]}"""
        val t = CgflixTrending.parse(json)!!
        assertEquals(listOf("a1", "a2", "a3"), t.idsFor(CgflixCategory.ANIMES))
        assertNull(t.idsFor(CgflixCategory.SERIES))
        assertEquals(CGFLIX_TRENDING_TITLE, t.title)
    }

    @Test
    fun `sem biblioteca nenhuma Series e Animes somem`() {
        val json = """{"itens":[{"id":"1","tipo":"Movie"},{"id":"2","tipo":"Movie"},{"id":"3"},{"id":"4","tipo":"Series"}]}"""
        val t = CgflixTrending.parse(json)!!
        assertEquals(listOf("1", "2", "3"), t.idsFor(CgflixCategory.FILMES))
        assertNull(t.idsFor(CgflixCategory.SERIES))
        assertNull(t.idsFor(CgflixCategory.ANIMES))
    }

    @Test
    fun `JSON quebrado ou vazio vira nulo`() {
        assertNull(CgflixTrending.parse("<html>404</html>"))
        assertNull(CgflixTrending.parse("""{"itens":[]}"""))
        assertNull(CgflixTrending.parse("[]"))
    }

    @Test
    fun `reordena pelo ranking e ignora o que o servidor nao devolveu`() {
        val items = listOf("CCCC-1", "aaaa1", "bbbb1")
        val ordered = cgflixOrderByIds(items, listOf("bbbb1", "zzz", "AAAA1", "cccc1"), { it })
        assertEquals(listOf("bbbb1", "aaaa1", "CCCC-1"), ordered)
    }

    @Test
    fun `cache vale 1 hora`() {
        var now = 0L
        val cache = CgflixTtlCache<String>(CGFLIX_TRENDING_TTL_MS) { now }
        cache.put("x")
        now = CGFLIX_TRENDING_TTL_MS - 1
        assertEquals("x", cache.get())
        now = CGFLIX_TRENDING_TTL_MS
        assertNull(cache.get())
    }
}
