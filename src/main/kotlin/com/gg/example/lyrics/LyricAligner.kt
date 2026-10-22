package com.gg.example.lyrics

import java.util.SortedMap
import kotlin.math.abs

/** Existing matching policy, extracted without changing thresholds or candidate order. */
internal class LyricAligner {
    private val punctuation = Regex("[^\\p{L}\\p{N}]")

    fun findMatch(line: YrcLine, originals: SortedMap<Long, String>): Long? {
        val candidates = originals.filterKeys { abs(it - line.startMs) < 5_000 }
        if (candidates.isEmpty()) return null

        val normalized = normalize(line.text)
        candidates.entries.firstOrNull { normalize(it.value) == normalized }?.let { return it.key }
        candidates.entries.firstOrNull {
            val original = normalize(it.value)
            original.isNotBlank() && normalized.isNotBlank() &&
                (original.contains(normalized) || normalized.contains(original))
        }?.let { return it.key }

        return candidates.keys.minByOrNull { abs(it - line.startMs) }
            ?.takeIf { abs(it - line.startMs) <= 1_000 }
    }

    private fun normalize(text: String): String = text.replace(punctuation, "").lowercase()
}
