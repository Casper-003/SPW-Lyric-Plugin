package com.gg.example.lyrics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LyricAlignerTest {
    private val aligner = LyricAligner()
    private fun line(time: Long, text: String) = YrcLine(time, 1_000, listOf(YrcWord(time, text)))

    @Test
    fun `matches normalized text before the nearest timestamp`() {
        assertEquals(
            11_000L,
            aligner.findMatch(line(10_000, "Hello, WORLD!"), sortedMapOf(10_000L to "other", 11_000L to "hello world")),
        )
    }

    @Test
    fun `matches a split sentence by substring`() {
        assertEquals(10_000L, aligner.findMatch(line(11_000, "Hello"), sortedMapOf(10_000L to "Hello world")))
    }

    @Test
    fun `only permits one second of timestamp fallback`() {
        val originals = sortedMapOf(10_000L to "different")
        assertEquals(10_000L, aligner.findMatch(line(11_000, "Hello"), originals))
        assertNull(aligner.findMatch(line(11_001, "Hello"), originals))
    }

    @Test
    fun `excludes text matches five seconds away`() {
        assertNull(aligner.findMatch(line(15_000, "Hello"), sortedMapOf(10_000L to "Hello")))
    }
}
