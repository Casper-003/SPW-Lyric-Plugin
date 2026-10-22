package com.gg.example.lyrics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YrcParserTest {
    @Test
    fun `parses word positions and line duration without network or SPW`() {
        val lines = YrcParser().parse(
            "{\"t\":0,\"c\":[]}\r\n[10000,2000](10000,1000,0)Hello (11000,1000,0)world",
        )
        assertEquals(
            listOf(YrcLine(10_000, 2_000, listOf(YrcWord(10_000, "Hello "), YrcWord(11_000, "world")))),
            lines,
        )
        assertEquals("Hello world", lines.single().text)
    }
}
