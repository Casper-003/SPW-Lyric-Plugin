package com.gg.example.lyrics

internal class MetadataClassifier {
    private val prefixes = listOf(
        "作词", "作曲", "编曲", "制作人", "混音", "母带", "和声", "吉他", "贝斯", "鼓", "弦乐", "录音",
        "企划", "监制", "出品", "统筹", "后期", "作詞", "編曲", "vocal", "lyric", "lyrics",
        "composer", "arrangement", "arrange", "mix", "master", "producer", "illustration", "movie", "director", "staff",
    )

    fun isMetadata(text: String): Boolean {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return false
        val lowerText = cleanText.lowercase()
        if (listOf("lyrics by", "composed by", "arranged by").any(lowerText::startsWith)) return true

        for (prefix in prefixes) {
            if (cleanText.startsWith(prefix, ignoreCase = true)) {
                val after = cleanText.substring(prefix.length).trimStart()
                if (after.isEmpty() || after.startsWith(":") || after.startsWith("：") || after.startsWith(" ")) {
                    return true
                }
            }
        }
        return listOf("词", "曲", "唱", "编").any { prefix ->
            if (!cleanText.startsWith(prefix)) return@any false
            val after = cleanText.substring(prefix.length).trimStart()
            after.startsWith(":") || after.startsWith("：")
        }
    }
}
