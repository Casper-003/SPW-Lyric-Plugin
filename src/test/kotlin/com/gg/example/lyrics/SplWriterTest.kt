package com.gg.example.lyrics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SplWriterTest {
    private val writer = SplWriter()

    @Test
    fun `writes millisecond precision across minute boundaries`() {
        assertEquals("00:00.000", writer.timestamp(0))
        assertEquals("01:01.007", writer.timestamp(61_007))
        assertEquals("[01:01.007]Hello\n", writer.timedLine(61_007, "Hello"))
    }

    @Test
    fun `writes first word delay subsequent starts and line end`() {
        assertEquals(
            "[00:10.000]<00:10.200>Hello <00:11.000>world[00:12.000]\n",
            writer.wordSyncedLine(
                YrcLine(10_000, 2_000, listOf(YrcWord(10_200, "Hello "), YrcWord(11_000, "world"))),
            ),
        )
    }
}
