package com.gg.example.lyrics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/** Desired behavior for confirmed defects. Enable one test when its separate fix is implemented and validated. */
class KnownLyricIssuesTest {
    private val merger = LyricMerger()

    @Test
    @Disabled("LYRIC-1: repeated text selects the earliest candidate; timeline fixes are deferred")
    fun `repeated lyrics should use the translation at the exact timestamp`() {
        assertEquals(
            "[00:12.000]la[00:13.000]\ntranslation-B\n",
            merger.merge(
                LyricPayload(
                    original = "[00:10.00]la\n[00:12.00]la",
                    translation = "[00:10.00]translation-A\n[00:12.00]translation-B",
                    wordSynced = "[12000,1000](12000,1000,0)la",
                ),
            ),
        )
    }

    @Test
    @Disabled("LYRIC-2: translation timestamps must currently be identical; alignment policy needs local validation")
    fun `a small translation offset should not discard the translation`() {
        assertEquals(
            "[00:10.000]Hello[00:11.000]\ntranslation\n",
            merger.merge(
                LyricPayload("[00:10.00]Hello", "[00:10.20]translation", wordSynced = "[10000,1000](10000,1000,0)Hello"),
            ),
        )
    }

    @Test
    @Disabled("LYRIC-3: a YRC line without words currently suppresses valid LRC fallback")
    fun `YRC without words should fall back to ordinary lyrics`() {
        assertEquals(
            "[00:10.000]Hello\ntranslation\n",
            merger.merge(LyricPayload("[00:10.00]Hello", "[00:10.00]translation", wordSynced = "[10000,1000]")),
        )
    }

    @Test
    @Disabled("LYRIC-4: only the first LRC timestamp on a line is currently parsed")
    fun `multiple LRC timestamps should produce separate occurrences`() {
        assertEquals(
            "[00:10.000]Echo\n[00:20.000]Echo\n",
            merger.merge(LyricPayload(original = "[00:10.00][00:20.00]Echo")),
        )
    }
}
