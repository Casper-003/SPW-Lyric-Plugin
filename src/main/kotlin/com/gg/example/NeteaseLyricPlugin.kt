@file:OptIn(UnstableSpwWorkshopApi::class)

package com.gg.example

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint
import com.xuncorp.spw.workshop.api.WorkshopApi
import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi
import com.xuncorp.spw.workshop.api.config.ConfigHelper
import org.pf4j.Extension
import org.pf4j.Plugin
import org.pf4j.PluginWrapper
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.TreeMap
import java.util.concurrent.TimeUnit

class NeteaseLyricPlugin(wrapper: PluginWrapper) : Plugin(wrapper) {

    @Extension
    class LyricProvider : PlaybackExtensionPoint {

        // 1. 初始化网络请求客户端
        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        // 2. 初始化配置读取器 (专门负责读取 preference_config.json 里的设置)
        private val configManager = WorkshopApi.manager.createConfigManager()
        private val configHelper: ConfigHelper = configManager.getConfig()

        override fun onAfterLoadLyrics(mediaItem: PlaybackExtensionPoint.MediaItem): String? {
            val title = mediaItem.title
            val artist = mediaItem.artist

            val originalKeyword = "$title $artist".trim()
            if (originalKeyword.isBlank()) return null

            WorkshopApi.ui.toast("正在匹配: $title", WorkshopApi.Ui.ToastType.Success)

            try {
                var songId = trySearchNeteaseId(originalKeyword)

                // 如果原名搜不到，尝试清洗歌名里的杂质再搜一次
                if (songId == null) {
                    var cleanKeyword = originalKeyword.replace(Regex("\\[\\(【（].*?[\\]\\)】）]"), "")
                    cleanKeyword = cleanKeyword.replace(Regex("(?i)(feat\\.|remix|cover|live|伴奏|无损|高音质|动态翻译)"), "")
                    cleanKeyword = cleanKeyword.replace(Regex("\\s+"), " ").trim()

                    if (cleanKeyword.isNotBlank() && cleanKeyword != originalKeyword) {
                        songId = trySearchNeteaseId(cleanKeyword)
                    }
                }

                if (songId == null) {
                    WorkshopApi.ui.toast("未找到歌曲: $title", WorkshopApi.Ui.ToastType.Warning)
                    return null
                }

                // 拿着找到的 ID 去获取并合并歌词
                val finalLrcText = fetchAndMergeLyrics(songId)

                if (!finalLrcText.isNullOrBlank()) {
                    WorkshopApi.ui.toast("匹配成功!", WorkshopApi.Ui.ToastType.Success)
                    return finalLrcText
                }

            } catch (e: Exception) {
                WorkshopApi.ui.toast("匹配出错: ${e.message}", WorkshopApi.Ui.ToastType.Error)
                e.printStackTrace()
            }
            return null
        }

        private fun trySearchNeteaseId(keyword: String): Long? {
            val bodyString = "s=$keyword&type=1&offset=0&limit=1"
            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            val requestBody = bodyString.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("http://music.163.com/api/search/get/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "http://music.163.com/")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val responseBodyString = response.body?.string() ?: return null
            val json = JsonParser.parseString(responseBodyString).asJsonObject
            val result = json.getAsJsonObject("result")
            val songs = result?.getAsJsonArray("songs")

            if (songs != null && !songs.isEmpty) {
                return songs.get(0).asJsonObject.get("id").asLong
            }
            return null
        }

        private fun fetchAndMergeLyrics(songId: Long): String? {
            val lyricUrl = "http://music.163.com/api/song/lyric?id=$songId&lv=1&tv=-1&rv=-1"
            val request = Request.Builder().url(lyricUrl).get().build()
            val response = httpClient.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: return null

            val json = JsonParser.parseString(responseBodyString).asJsonObject

            val originalLrc = json.getAsJsonObject("lrc")?.get("lyric")?.asString ?: ""
            val transLrc = json.getAsJsonObject("tlyric")?.get("lyric")?.asString ?: ""
            val romaLrc = json.getAsJsonObject("romalrc")?.get("lyric")?.asString ?: ""

            // 3. 核心开关逻辑：每次请求歌词前，重新读取用户当前的设置
            configHelper.reload()

            val useTrans = configHelper.get("enable_translation", true)
            val useRoma = configHelper.get("enable_roman", false)

            val timeDict = TreeMap<Long, MutableList<String>>()

            fun parseToDict(lrcStr: String?) {
                if (lrcStr.isNullOrBlank()) return
                val lines = lrcStr.split("\r\n", "\n", "\r")
                val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

                for (line in lines) {
                    val match = regex.find(line)
                    if (match != null) {
                        val (m, s, msStr, text) = match.destructured
                        val cleanText = text.trim()
                        if (cleanText.isBlank()) continue

                        val ms = if (msStr.length == 2) msStr.toInt() * 10 else msStr.toInt()
                        val totalMs = m.toLong() * 60000 + s.toLong() * 1000 + ms

                        val textList = timeDict.getOrPut(totalMs) { mutableListOf() }
                        if (!textList.contains(cleanText)) {
                            textList.add(cleanText)
                        }
                    }
                }
            }

            // 4. 根据开关决定是否解析合并特定的歌词
            parseToDict(originalLrc)
            if (useRoma) parseToDict(romaLrc)     // 只有开关开启才合并罗马音
            if (useTrans) parseToDict(transLrc)   // 只有开关开启才合并翻译

            if (timeDict.isEmpty()) return null

            val sb = StringBuilder()
            for ((timeMs, textList) in timeDict) {
                val m = timeMs / 60000
                val s = (timeMs % 60000) / 1000
                val ms = timeMs % 1000
                val timeTag = String.format("[%02d:%02d.%03d]", m, s, ms)
                for (textLine in textList) {
                    sb.append(timeTag).append(textLine).append("\n")
                }
            }
            return sb.toString()
        }
    }
}