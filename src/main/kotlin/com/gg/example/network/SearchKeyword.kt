package com.gg.example.network

internal object SearchKeyword {
    /** Existing fallback cleanup; matching policy changes are deferred to a separate fix. */
    fun clean(keyword: String): String = keyword
        .replace(Regex("\\[\\(【（].*?[\\]\\)】）]"), "")
        .replace(Regex("(?i)(feat\\.|remix|cover|live|伴奏|无损|高音质|动态翻译)"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
