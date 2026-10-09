package dev.jdtech.jellyfin.cgflix.logic

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CgflixSeerrTest {
    private val jellyfin = "https://netflix.docaio.com.br"
    private val seerr = "https://pedidos.docaio.com.br"

    private class MemoryStore : CgflixSessionStore {
        val map = HashMap<String, String>()

        override fun read(key: String) = map[key]

        override fun write(key: String, value: String) {
            map[key] = value
        }

        override fun delete(key: String) {
            map.remove(key)
        }
    }

    /** Seerr falso: responde por método + caminho; registra o que foi pedido. */
    private class FakeSeerr(val base: String) : CgflixHttp {
        var up = true
        var validCookie = "connect.sid=novo"
        var searchBody = "{\"results\":[]}"
        val calls = mutableListOf<CgflixHttpRequest>()
        var authenticateCount = 0

        override suspend fun send(request: CgflixHttpRequest): CgflixHttpResponse {
            calls += request
            if (!request.url.startsWith(base) || !up) throw IOException("sem rota")
            val path = request.url.removePrefix("$base/api/v1")
            return when {
                path == "/settings/public" -> CgflixHttpResponse(200, "{\"initialized\":true}")
                path == "/auth/jellyfin/quickconnect/initiate" ->
                    CgflixHttpResponse(200, "{\"code\":\"123456\",\"secret\":\"segredo\"}")
                path == "/auth/jellyfin/quickconnect/authenticate" -> {
                    authenticateCount++
                    CgflixHttpResponse(200, "{\"id\":7}", listOf("$validCookie; Path=/; HttpOnly"))
                }
                request.cookie != validCookie -> CgflixHttpResponse(401, "{}")
                path.startsWith("/search") -> CgflixHttpResponse(200, searchBody)
                path == "/request" -> CgflixHttpResponse(201, "{}")
                path.startsWith("/user/7/requests") ->
                    CgflixHttpResponse(
                        200,
                        """{"results":[
                           {"id":1,"status":1,"createdAt":"2026-10-01","media":{"tmdbId":10,"mediaType":"movie","status":2}},
                           {"id":2,"status":2,"createdAt":"2026-10-05","media":{"tmdbId":20,"mediaType":"tv","status":5}}]}""",
                    )
                path.startsWith("/movie/10") ->
                    CgflixHttpResponse(
                        200,
                        """{"title":"Filme Pedido","releaseDate":"2024-01-01","posterPath":"/abc.jpg"}""",
                    )
                path.startsWith("/tv/20") ->
                    CgflixHttpResponse(
                        200,
                        """{"name":"Série Pedida","firstAirDate":"2020-05-01","posterPath":"../x.jpg"}""",
                    )
                else -> CgflixHttpResponse(404, "{}")
            }
        }
    }

    private fun client(
        fake: FakeSeerr,
        store: CgflixSessionStore = MemoryStore(),
        authorized: MutableList<String> = mutableListOf(),
    ) =
        CgflixSeerrClient(
            jellyfinBaseUrl = jellyfin,
            accountKey = "srv:user",
            http = fake,
            store = store,
            authorizeQuickConnect = { authorized += it },
            retryDelayMs = 0,
        )

    private fun result(id: Int, type: String, status: Int?) =
        """{"id":$id,"mediaType":"$type","title":"T$id","name":"T$id","posterPath":"/p$id.jpg"""" +
            (if (status == null) "}" else ""","mediaInfo":{"status":$status}}""")

    @Test
    fun `entra sozinho pelo Quick Connect e mostra so o que nao esta disponivel`() = runBlocking {
        val fake = FakeSeerr(seerr)
        fake.searchBody =
            """{"results":[${result(1, "movie", null)},${result(2, "tv", 1)},${result(3, "movie", 2)},
               ${result(4, "tv", 3)},${result(5, "movie", 4)},${result(6, "movie", 5)},
               {"id":9,"mediaType":"person","name":"Fulano"}]}"""
        val authorized = mutableListOf<String>()
        val store = MemoryStore()
        val items = client(fake, store, authorized).search("teste")

        assertEquals(listOf("123456"), authorized) // aprovou o código no Jellyfin
        assertEquals(listOf(1, 2, 3, 4), items.map { it.tmdbId }) // 4/5 e pessoas não aparecem
        assertEquals(CgflixRequestState.REQUESTABLE, items[0].state)
        assertEquals(CgflixRequestState.REQUESTABLE, items[1].state)
        assertEquals(CgflixRequestState.REQUESTED, items[2].state)
        assertEquals(CgflixRequestState.DOWNLOADING, items[3].state)
        assertEquals("$seerr/imageproxy/tmdb/t/p/w300_and_h450_face/p1.jpg", items[0].posterUrl)
        assertTrue(store.map.values.single().contains("connect.sid=novo"))
        // a busca vai com language=pt-BR e o cookie
        val search = fake.calls.last()
        assertTrue(search.url.endsWith("/search?query=teste&page=1&language=pt-BR"))
        assertEquals("connect.sid=novo", search.cookie)
    }

    @Test
    fun `pedido de filme vai direto e de serie pede todas as temporadas`() = runBlocking {
        val fake = FakeSeerr(seerr)
        val c = client(fake)
        c.request(CgflixRequestable(10, true, "Filme"))
        assertEquals("""{"mediaType":"movie","mediaId":10}""", fake.calls.last().jsonBody)
        c.request(CgflixRequestable(20, false, "Série"))
        assertEquals(
            """{"mediaType":"tv","mediaId":20,"seasons":"all"}""",
            fake.calls.last().jsonBody,
        )
    }

    @Test
    fun `Seerr fora do ar deixa os pedidos indisponiveis sem tentar de novo na hora`() =
        runBlocking {
            val fake = FakeSeerr(seerr).apply { up = false }
            val c = client(fake)
            try {
                c.search("x")
                fail("devia ficar indisponível")
            } catch (_: CgflixRequestsUnavailable) {}
            val callsAfterFirst = fake.calls.size
            try {
                c.search("y")
                fail("devia ficar indisponível")
            } catch (_: CgflixRequestsUnavailable) {}
            assertEquals(callsAfterFirst, fake.calls.size) // espera 2 min antes de tentar de novo
        }

    @Test
    fun `cookie vencido 401 renova sozinho e repete a chamada`() = runBlocking {
        val fake = FakeSeerr(seerr)
        val store = MemoryStore()
        store.write(
            "seerr_session:srv:user",
            """{"url":"$seerr","cookie":"connect.sid=velho","user":7}""",
        )
        fake.searchBody = """{"results":[${result(1, "movie", null)}]}"""
        val items = client(fake, store).search("x")
        assertEquals(1, items.size)
        assertEquals(1, fake.authenticateCount)
        assertTrue(store.map.values.single().contains("connect.sid=novo"))
        assertFalse(store.map.values.single().contains("velho"))
    }

    @Test
    fun `Meus pedidos com nome situacao e poster so com caminho valido`() = runBlocking {
        val list = client(FakeSeerr(seerr)).myRequests()
        assertEquals(
            listOf("Série Pedida", "Filme Pedido"),
            list.map { it.title },
        ) // mais novo primeiro
        assertEquals(CgflixMyRequestStatus.AVAILABLE, list[0].status)
        assertEquals(CgflixMyRequestStatus.WAITING_APPROVAL, list[1].status)
        assertNull(list[0].posterUrl) // "../x.jpg" não passa na regra
        assertEquals("$seerr/imageproxy/tmdb/t/p/w300_and_h450_face/abc.jpg", list[1].posterUrl)
        assertEquals(2024, list[1].year)
    }

    @Test
    fun `endereco do Seerr derivado do Jellyfin`() {
        assertEquals(seerr, cgflixSeerrCandidates(jellyfin).first())
        assertEquals(
            listOf("http://192.168.0.10:5055"),
            cgflixSeerrCandidates("http://192.168.0.10:8096"),
        )
        assertTrue(cgflixSeerrCandidates("lixo sem host").isEmpty())
    }

    @Test
    fun `regra do poster igual a do site`() {
        assertEquals(
            "$seerr/imageproxy/tmdb/t/p/w300_and_h450_face/a_b-1.webp",
            cgflixSeerrPoster(seerr, "/a_b-1.webp"),
        )
        assertNull(cgflixSeerrPoster(seerr, "/a/b.jpg"))
        assertNull(cgflixSeerrPoster(seerr, "https://evil/x.jpg"))
        assertNull(cgflixSeerrPoster(seerr, "/x.gif"))
        assertNull(cgflixSeerrPoster(seerr, null))
    }

    @Test
    fun `situacoes de Meus pedidos`() {
        assertEquals(CgflixMyRequestStatus.DECLINED, cgflixMyRequestStatusFor(3, null))
        assertEquals(CgflixMyRequestStatus.DOWNLOADING, cgflixMyRequestStatusFor(2, 3))
        assertEquals(CgflixMyRequestStatus.APPROVED, cgflixMyRequestStatusFor(2, 2))
        assertEquals(CgflixMyRequestStatus.PARTIALLY_AVAILABLE, cgflixMyRequestStatusFor(2, 4))
    }

    @Test
    fun `le so o connect sid do Set-Cookie`() {
        assertEquals(
            "connect.sid=s%3Aabc",
            CgflixSeerrClient.sessionCookieOf("connect.sid=s%3Aabc; Path=/; HttpOnly"),
        )
        assertNull(CgflixSeerrClient.sessionCookieOf("outro=1; Path=/"))
    }
}
