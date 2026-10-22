package com.gg.example.lyrics

/** Coordinates parsing, the existing alignment policy, and SPL serialization. No SPW or HTTP dependency. */
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
        val yrcLines = if (options.wordSynced && payload.wordSynced.isNotBlank()) {
            yrcParser.parse(payload.wordSynced)
        } else {
            emptyList()
        }

        return buildString {
            if (yrcLines.isNotEmpty()) {
                for (line in yrcLines) {
                    append(writer.wordSyncedLine(line))
                    val time = aligner.findMatch(line, originals) ?: continue
                    if (!metadata.isMetadata(line.text)) {
                        romanizations[time]?.let { append(it).append('\n') }
                        translations[time]?.let { append(it).append('\n') }
                    }
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
}
