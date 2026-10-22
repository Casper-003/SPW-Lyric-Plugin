package com.gg.example.lyrics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LrcParserTest {
    private val parser = LrcParser()

    @Test
    fun `parses fractions and sorts timestamps across newline styles`() {
        val source = "[00:12.345] second\r\n[00:10.20] first\r[00:13.00] third\n[ar:artist]"
        assertEquals(
            linkedMapOf(10_200L to "first", 12_345L to "second", 13_000L to "third"),
            parser.parse(source),
        )
        assertEquals(listOf(10_200L, 12_345L, 13_000L), parser.parse(source).keys.toList())
    }

    @Test
    fun `combines duplicate timestamps and ignores empty or untimed text`() {
        assertEquals(
            mapOf(10_000L to "Hello world"),
            parser.parse("[00:10.00]Hello\n[00:10.000] world\n[00:11.00]  \nuntimed"),
        )
    }

    @Test
    fun `accepts absent lyrics`() {
        assertTrue(parser.parse(null).isEmpty())
        assertTrue(parser.parse(" \n").isEmpty())
    }
}
