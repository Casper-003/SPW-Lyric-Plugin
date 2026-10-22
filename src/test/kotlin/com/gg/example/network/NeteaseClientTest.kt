package com.gg.example.network

import com.gg.example.lyrics.LyricMerger
import com.gg.example.lyrics.LyricPayload
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class NeteaseClientTest {
    private val server = MockWebServer()
    private lateinit var client: NeteaseClient

    @BeforeEach
    fun setUp() {
        server.start()
        client = NeteaseClient(baseUrl = server.url("/"))
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `searches then fetches and merges lyrics through actual HTTP requests`() {
        server.enqueue(MockResponse().setBody("""{"result":{"songs":[{"id":42},{"id":43}]}}"""))
        server.enqueue(MockResponse().setBody("""{"lrc":{"lyric":"[00:10.00]Hello"},"tlyric":{"lyric":"[00:10.00]你好"}}"""))

        val id = requireNotNull(client.searchSongId("Hello Artist"))
        assertEquals(42L, id)
        val payload = requireNotNull(client.fetchLyrics(id))
        assertEquals("[00:10.000]Hello\n你好\n", LyricMerger().merge(payload))

        val search = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        assertEquals("POST", search.method)
        assertEquals("/api/search/get/", search.path)
        assertEquals("s=Hello Artist&type=1&offset=0&limit=1", search.body.readUtf8())
        assertEquals("application/x-www-form-urlencoded; charset=utf-8", search.getHeader("Content-Type"))
        val lyrics = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        assertEquals("GET", lyrics.method)
        assertEquals("/api/song/lyric?id=42&lv=1&tv=-1&rv=-1&yv=1", lyrics.path)
    }

    @Test
    fun `returns no song for a failed search or empty results`() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("unavailable"))
        server.enqueue(MockResponse().setBody("""{"result":{"songs":[]}}"""))
        server.enqueue(MockResponse().setBody("{}"))
        repeat(3) { assertNull(client.searchSongId("Hello")) }
    }

    @Test
    fun `extracts every supported lyric stream`() {
        server.enqueue(MockResponse().setBody("""{"lrc":{"lyric":"original"},"tlyric":{"lyric":"translation"},"romalrc":{"lyric":"roman"},"yrc":{"lyric":"words"}}"""))
        assertEquals(LyricPayload("original", "translation", "roman", "words"), client.fetchLyrics(42))
    }

    @Test
    fun `treats absent lyric streams as empty`() {
        server.enqueue(MockResponse().setBody("{}"))
        assertEquals(LyricPayload(), client.fetchLyrics(42))
    }

    @Test
    fun `surfaces malformed JSON for the plugin error handler`() {
        server.enqueue(MockResponse().setBody("<html>error</html>"))
        assertThrows(RuntimeException::class.java) { client.fetchLyrics(42) }
    }
}
