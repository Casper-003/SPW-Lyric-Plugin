package com.gg.example.lyrics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpeningTimingTest {
    private val merger = LyricMerger()

    // Synthetic text with the observed timing of song 2080448369; no copyrighted lyric fixture.
    private fun payload(firstStart: Long = 4_870) = LyricPayload(
        original = "[00:00.00]作词 : Author\n[00:00.00]作曲 : Composer\n[00:00.00]Song - Singer\n" +
            "[00:00.62]词：Author\n[00:00.89]曲：Composer\n[00:01.27]Opening\n[00:23.21]Second",
        translation = "[00:01.27]First translation\n[00:23.21]Second translation",
        wordSynced = "[0,373](0,373,0)作词 : Author\n[373,373](373,373,0)作曲 : Composer\n" +
            "[1120,2100](1120,2100,0)Song - Singer\n[3220,820](3220,820,0)词：Author\n" +
            "[4040,830](4040,830,0)曲：Composer\n" +
            "[$firstStart,18250]($firstStart,1520,0)Opening\n[23120,10910](23120,970,0)Second",
    )

    @Test
    fun `conflicting opening uses the complete ordinary timeline`() {
        val source = payload()
        assertEquals(merger.merge(source, LyricOptions(wordSynced = false)), merger.merge(source))
        assertTrue(merger.merge(source)!!.contains("[00:01.270]Opening\nFirst translation\n"))
    }

    @Test
    fun `timing fallback is independent of translation display`() {
        val source = payload()
        assertEquals(
            merger.merge(source, LyricOptions(translation = false, wordSynced = false)),
            merger.merge(source, LyricOptions(translation = false)),
        )
    }

    @Test
    fun `credits leave the original title slot available and keep normal word timing`() {
        val output = merger.merge(payload(firstStart = 1_270))!!
        assertTrue(output.contains("[00:01.270]Opening[00:19.520]\nFirst translation\n"))
        assertTrue(output.contains("[00:23.120]Second[00:34.030]\nSecond translation\n"))
    }

    @Test
    fun `repeated text does not trigger a speculative timing fallback`() {
        val source = payload().copy(
            original = "[00:01.27]Opening\n[00:10.00]Opening\n[00:23.21]Second",
        )
        assertTrue(merger.merge(source)!!.contains("[00:04.870]Opening[00:23.120]"))
    }
}
