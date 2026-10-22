package com.gg.example.lyrics

internal class SplWriter {
    fun timestamp(timeMs: Long): String {
        val seconds = timeMs / 1_000
        return String.format("%02d:%02d.%03d", seconds / 60, seconds % 60, timeMs % 1_000)
    }

    fun timedLine(timeMs: Long, text: String): String = "[${timestamp(timeMs)}]$text\n"

    fun wordSyncedLine(line: YrcLine): String = buildString {
        append("[").append(timestamp(line.startMs)).append("]")
        line.words.forEachIndexed { index, word ->
            if (index != 0 || word.startMs > line.startMs) {
                append("<").append(timestamp(word.startMs)).append(">")
            }
            append(word.text)
        }
        append("[").append(timestamp(line.startMs + line.durationMs)).append("]\n")
    }
}
