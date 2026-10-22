package com.gg.example.lyrics

import kotlin.math.abs

/** Coordinates parsing, timing validation, alignment, and SPL serialization. */
internal class LyricMerger(
    private val lrcParser: LrcParser = LrcParser(),
    private val yrcParser: YrcParser = YrcParser(),
    private val aligner: LyricAligner = LyricAligner(),
    private val writer: SplWriter = SplWriter(),
    private val metadata: MetadataClassifier = MetadataClassifier(),
) {
    fun merge(payload: LyricPayload, options: LyricOptions = LyricOptions()): String? {
        val originals = lrcParser.parse(payload.original)
        val translations = lrcParser.parse(payload.translation.takeIf { options.translation })
        val romanizations = lrcParser.parse(payload.romanization.takeIf { options.romanization })
        val parsedYrc = if (options.wordSynced && payload.wordSynced.isNotBlank()) {
            yrcParser.parse(payload.wordSynced)
        } else {
            emptyList()
        }
        val yrcLines = parsedYrc.takeUnless {
            hasConflictingOpening(it, originals, payload)
        }.orEmpty()

        return buildString {
            if (yrcLines.isNotEmpty()) {
                for (line in yrcLines) {
                    append(writer.wordSyncedLine(line))
                    // Credits must not consume an original/translation slot by timestamp proximity.
                    if (metadata.isMetadata(line.text)) continue
                    val time = aligner.findMatch(line, originals) ?: continue
                    romanizations[time]?.let { append(it).append('\n') }
                    translations[time]?.let { append(it).append('\n') }
                    // One LRC slot is consumed per YRC line, preserving the current behavior.
                    romanizations.remove(time)
                    translations.remove(time)
                    originals.remove(time)
                }
            } else {
                val times = sortedSetOf<Long>().apply {
                    addAll(originals.keys)
                    addAll(romanizations.keys)
                    addAll(translations.keys)
                }
                if (times.isEmpty()) return null
                for (time in times) {
                    val original = originals[time]
                    val romanization = romanizations[time]
                    val translation = translations[time]
                    when {
                        original != null -> {
                            append(writer.timedLine(time, original))
                            if (!metadata.isMetadata(original)) {
                                romanization?.let { append(it).append('\n') }
                                translation?.let { append(it).append('\n') }
                            }
                        }
                        romanization != null -> {
                            append(writer.timedLine(time, romanization))
                            translation?.let { append(it).append('\n') }
                        }
                        translation != null -> append(writer.timedLine(time, translation))
                    }
                }
            }
        }
    }

    private fun hasConflictingOpening(
        lines: List<YrcLine>,
        originals: Map<Long, String>,
        payload: LyricPayload,
    ): Boolean {
        if (lines.isEmpty()) return false
        // Supplementary text identifies sung lines rather than titles and credits. Inspect raw
        // payloads so the safety decision does not depend on the user's display switches.
        val sungTimes = lrcParser.parse(payload.translation).keys + lrcParser.parse(payload.romanization).keys
        val opening = originals.entries.filter { it.key in sungTimes && !metadata.isMetadata(it.value) }.take(2)
        if (opening.size < 2) return false
        val anchors = opening.map { original ->
            if (originals.values.count { aligner.hasSameText(it, original.value) } != 1) return false
            val line = lines.singleOrNull { aligner.hasSameText(it.text, original.value) } ?: return false
            line.startMs - original.key
        }
        // Reuse the existing one-second tolerance, require a later aligned anchor, and never
        // invent word timestamps. A confirmed conflict uses the complete ordinary timeline.
        return abs(anchors[0]) > 1_000 && abs(anchors[1]) <= 1_000
    }
}
