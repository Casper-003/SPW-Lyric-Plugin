package com.gg.example.lyrics

internal class YrcParser {
    private val linePattern = Regex("""\[(\d+),(\d+)\](.*)""")
    private val wordPattern = Regex("""\((\d+),(\d+),\d+\)([^(\n]*)""")

    fun parse(source: String): List<YrcLine> = source.split("\r\n", "\n", "\r").mapNotNull { line ->
        val match = linePattern.find(line) ?: return@mapNotNull null
        val (start, duration, content) = match.destructured
        val words = wordPattern.findAll(content).map { word ->
            val (wordStart, _, text) = word.destructured
            YrcWord(wordStart.toLong(), text)
        }.toList()
        // Retain empty word lists for compatibility; see KnownLyricIssuesTest.
        YrcLine(start.toLong(), duration.toLong(), words)
    }
}
