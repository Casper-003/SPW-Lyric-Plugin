package com.gg.example.network

import com.gg.example.lyrics.LyricPayload
import com.google.gson.JsonParser
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

internal class NeteaseClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: HttpUrl = "http://music.163.com/".toHttpUrl(),
) {
    fun searchSongId(keyword: String): Long? {
        // Keep request encoding and first-result selection unchanged in this structural refactor.
        val body = "s=$keyword&type=1&offset=0&limit=1"
            .toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val request = Request.Builder()
            .url(baseUrl.resolve("api/search/get/")!!)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Referer", "http://music.163.com/")
            .post(body)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val source = response.body?.string() ?: return null
            val songs = JsonParser.parseString(source).asJsonObject
                .getAsJsonObject("result")?.getAsJsonArray("songs")
            if (songs != null && !songs.isEmpty) songs[0].asJsonObject.get("id").asLong else null
        }
    }

    fun fetchLyrics(songId: Long): LyricPayload? {
        val url = baseUrl.resolve("api/song/lyric")!!.newBuilder()
            .addQueryParameter("id", songId.toString())
            .addQueryParameter("lv", "1")
            .addQueryParameter("tv", "-1")
            .addQueryParameter("rv", "-1")
            .addQueryParameter("yv", "1")
            .build()
        val request = Request.Builder().url(url).get().build()
        return httpClient.newCall(request).execute().use { response ->
            val source = response.body?.string() ?: return null
            val json = JsonParser.parseString(source).asJsonObject
            fun lyric(key: String): String = json.getAsJsonObject(key)?.get("lyric")?.asString ?: ""
            LyricPayload(lyric("lrc"), lyric("tlyric"), lyric("romalrc"), lyric("yrc"))
        }
    }
}
