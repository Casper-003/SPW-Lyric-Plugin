package com.gg.example.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SearchKeywordTest {
    @Test
    fun `retains existing fallback cleanup behavior including parentheses`() {
        assertEquals("Song () Artist", SearchKeyword.clean("Song (Live)  Artist"))
        assertEquals("Song Artist", SearchKeyword.clean("Song 无损 高音质 Artist"))
    }
}
