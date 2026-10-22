package com.gg.example.lyrics

import java.util.TreeMap

internal class LrcParser {
    private val timestamp = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

    fun parse(source: String?): TreeMap<Long, String> {
        val result = TreeMap<Long, String>()
        if (source.isNullOrBlank()) return result

        for (line in source.split("\r\n", "\n", "\r")) {
            val match = timestamp.find(line) ?: continue
            val (minutes, seconds, fraction, text) = match.destructured
            val cleanText = text.trim()
            if (cleanText.isBlank()) continue

            val millis = if (fraction.length == 2) fraction.toInt() * 10 else fraction.toInt()
            val time = minutes.toLong() * 60_000 + seconds.toLong() * 1_000 + millis
            result[time] = result[time]?.let { "$it $cleanText" } ?: cleanText
        }
        return result
    }
}
