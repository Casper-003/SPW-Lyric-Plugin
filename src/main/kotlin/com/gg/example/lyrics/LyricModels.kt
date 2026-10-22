package com.gg.example.lyrics

internal data class LyricPayload(
    val original: String = "",
    val translation: String = "",
    val romanization: String = "",
    val wordSynced: String = "",
)

internal data class LyricOptions(
    val translation: Boolean = true,
    val romanization: Boolean = false,
    val wordSynced: Boolean = true,
)

internal data class YrcWord(val startMs: Long, val text: String)

internal data class YrcLine(
    val startMs: Long,
    val durationMs: Long,
    val words: List<YrcWord>,
) {
    val text: String get() = words.joinToString("") { it.text }
}
