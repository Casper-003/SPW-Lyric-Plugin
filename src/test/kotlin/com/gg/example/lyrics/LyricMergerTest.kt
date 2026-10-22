package com.gg.example.lyrics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LyricMergerTest {
    private val merger = LyricMerger()

    @Test
    fun `merges original romanization and translation in existing order`() {
        val payload = LyricPayload("[00:10.00]こんにちは", "[00:10.00]你好", "[00:10.00]konnichiwa")
        assertEquals(
            "[00:10.000]こんにちは\nkonnichiwa\n你好\n",
            merger.merge(payload, LyricOptions(romanization = true)),
        )
        assertEquals(
            "[00:10.000]こんにちは\n",
            merger.merge(payload, LyricOptions(translation = false)),
        )
    }

    @Test
    fun `writes word synced lyrics and aligned translation`() {
        val payload = LyricPayload(
            original = "[00:10.00]Hello world",
            translation = "[00:10.00]你好世界",
            wordSynced = "[10000,2000](10000,1000,0)Hello (11000,1000,0)world",
        )
        assertEquals("[00:10.000]Hello <00:11.000>world[00:12.000]\n你好世界\n", merger.merge(payload))
        assertEquals("[00:10.000]Hello world\n你好世界\n", merger.merge(payload, LyricOptions(wordSynced = false)))
    }

    @Test
    fun `falls back when no YRC lines can be parsed`() {
        assertEquals(
            "[00:10.000]Hello\n",
            merger.merge(LyricPayload(original = "[00:10.00]Hello", wordSynced = "not YRC")),
        )
    }

    @Test
    fun `suppresses translation and romanization for credit lines`() {
        val payload = LyricPayload("[00:10.00]作词：某人", "[00:10.00]credit translation", "[00:10.00]credit roman")
        assertEquals("[00:10.000]作词：某人\n", merger.merge(payload, LyricOptions(romanization = true)))
        assertEquals(
            "[00:10.000]作词：某人[00:11.000]\n",
            merger.merge(payload.copy(wordSynced = "[10000,1000](10000,1000,0)作词：某人")),
        )
    }

    @Test
    fun `retains translation only and romanization only slots`() {
        assertEquals("[00:10.000]你好\n", merger.merge(LyricPayload(translation = "[00:10.00]你好")))
        assertEquals(
            "[00:10.000]hello\n你好\n",
            merger.merge(
                LyricPayload(translation = "[00:10.00]你好", romanization = "[00:10.00]hello"),
                LyricOptions(romanization = true),
            ),
        )
    }

    @Test
    fun `returns null when no enabled lyrics exist`() {
        assertNull(merger.merge(LyricPayload()))
        assertNull(merger.merge(LyricPayload(romanization = "[00:10.00]hello")))
    }
}
