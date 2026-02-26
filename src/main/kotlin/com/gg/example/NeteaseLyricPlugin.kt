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
import kotlin.math.abs

class NeteaseLyricPlugin(wrapper: PluginWrapper) : Plugin(wrapper) {

    @Extension
    class LyricProvider : PlaybackExtensionPoint {

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        private val configManager = WorkshopApi.manager.createConfigManager()
        private val configHelper: ConfigHelper = configManager.getConfig()

        override fun onAfterLoadLyrics(mediaItem: PlaybackExtensionPoint.MediaItem): String? {
            // 全局开关拦截
            configHelper.reload()
            val enablePlugin = configHelper.get("enable_plugin", true)
            if (!enablePlugin) {
                return null
            }

            val showToast = configHelper.get("show_toast", false)
            fun safeToast(msg: String, type: WorkshopApi.Ui.ToastType) {
                if (showToast) WorkshopApi.ui.toast(msg, type)
            }

            val title = mediaItem.title
            val artist = mediaItem.artist

            val originalKeyword = "$title $artist".trim()
            if (originalKeyword.isBlank()) return null

            safeToast("正在匹配: $title", WorkshopApi.Ui.ToastType.Success)

            try {
                var songId = trySearchNeteaseId(originalKeyword)
                if (songId == null) {
                    var cleanKeyword = originalKeyword.replace(Regex("\\[\\(【（].*?[\\]\\)】）]"), "")
                    cleanKeyword = cleanKeyword.replace(Regex("(?i)(feat\\.|remix|cover|live|伴奏|无损|高音质|动态翻译)"), "")
                    cleanKeyword = cleanKeyword.replace(Regex("\\s+"), " ").trim()
                    if (cleanKeyword.isNotBlank() && cleanKeyword != originalKeyword) {
                        songId = trySearchNeteaseId(cleanKeyword)
                    }
                }

                if (songId == null) {
                    safeToast("未找到歌曲: $title", WorkshopApi.Ui.ToastType.Warning)
                    return null
                }

                val finalLrcText = fetchAndMergeLyrics(songId)

                if (!finalLrcText.isNullOrBlank()) {
                    safeToast("匹配成功!", WorkshopApi.Ui.ToastType.Success)
                    return finalLrcText
                }

            } catch (e: Exception) {
                safeToast("匹配出错: ${e.message}", WorkshopApi.Ui.ToastType.Error)
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

        private fun formatSplTime(ms: Long): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val milliseconds = ms % 1000
            return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
        }

        // 面向结果这一块：属性标签鉴定器
        private fun isMetadataLine(text: String): Boolean {
            val cleanText = text.trim()
            if (cleanText.isEmpty()) return false

            val lowerText = cleanText.lowercase()
            if (lowerText.startsWith("lyrics by") || lowerText.startsWith("composed by") || lowerText.startsWith("arranged by")) {
                return true
            }

            val multiWordPrefixes = listOf(
                "作词", "作曲", "编曲", "制作人", "混音", "母带", "和声", "吉他", "贝斯", "鼓", "弦乐", "录音",
                "企划", "监制", "出品", "统筹", "后期", "作詞", "編曲", "vocal", "lyric", "lyrics",
                "composer", "arrangement", "arrange", "mix", "master", "producer", "illustration", "movie", "director", "staff"
            )
            for (p in multiWordPrefixes) {
                if (cleanText.startsWith(p, ignoreCase = true)) {
                    val after = cleanText.substring(p.length).trimStart()
                    if (after.isEmpty() || after.startsWith(":") || after.startsWith("：") || after.startsWith(" ")) {
                        return true
                    }
                }
            }

            val singleCharPrefixes = listOf("词", "曲", "唱", "编")
            for (p in singleCharPrefixes) {
                if (cleanText.startsWith(p)) {
                    val after = cleanText.substring(p.length).trimStart()
                    if (after.startsWith(":") || after.startsWith("：")) {
                        return true
                    }
                }
            }
            return false
        }

        private fun fetchAndMergeLyrics(songId: Long): String? {
            val lyricUrl = "http://music.163.com/api/song/lyric?id=$songId&lv=1&tv=-1&rv=-1&yv=1"
            val request = Request.Builder().url(lyricUrl).get().build()
            val response = httpClient.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: return null

            val json = JsonParser.parseString(responseBodyString).asJsonObject

            val originalLrc = json.getAsJsonObject("lrc")?.get("lyric")?.asString ?: ""
            val transLrc = json.getAsJsonObject("tlyric")?.get("lyric")?.asString ?: ""
            val romaLrc = json.getAsJsonObject("romalrc")?.get("lyric")?.asString ?: ""
            val yrcLrc = json.getAsJsonObject("yrc")?.get("lyric")?.asString ?: ""

            configHelper.reload()
            val useTrans = configHelper.get("enable_translation", true)
            val useRoma = configHelper.get("enable_roman", false)
            val useYrc = configHelper.get("enable_yrc", true)

            fun parseLrcToMap(lrcStr: String?, map: TreeMap<Long, String>) {
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
                        map[totalMs] = if (map.containsKey(totalMs)) map[totalMs] + " " + cleanText else cleanText
                    }
                }
            }

            val origMap = TreeMap<Long, String>()
            val transMap = TreeMap<Long, String>()
            val romaMap = TreeMap<Long, String>()

            parseLrcToMap(originalLrc, origMap)
            if (useTrans) parseLrcToMap(transLrc, transMap)
            if (useRoma) parseLrcToMap(romaLrc, romaMap)

            val sb = StringBuilder()
            var hasProcessedYrc = false

            if (useYrc && yrcLrc.isNotBlank()) {
                val lines = yrcLrc.split("\r\n", "\n", "\r")
                val lineRegex = Regex("""\[(\d+),(\d+)\](.*)""")
                val wordRegex = Regex("""\((\d+),(\d+),\d+\)([^(\n]*)""")

                for (line in lines) {
                    val lineMatch = lineRegex.find(line) ?: continue
                    val (lineStartMsStr, lineDurationStr, content) = lineMatch.destructured
                    val lineStartMs = lineStartMsStr.toLong()
                    val lineDuration = lineDurationStr.toLong()

                    val splLineBuilder = StringBuilder()
                    splLineBuilder.append("[").append(formatSplTime(lineStartMs)).append("]")

                    val words = wordRegex.findAll(content)
                    var isFirstWord = true
                    var yrcTextOnly = ""

                    for (wordMatch in words) {
                        val (wordStartMsStr, _, wordText) = wordMatch.destructured
                        val wordStartMs = wordStartMsStr.toLong()
                        if (!isFirstWord || wordStartMs > lineStartMs) {
                            splLineBuilder.append("<").append(formatSplTime(wordStartMs)).append(">")
                        }
                        splLineBuilder.append(wordText)
                        yrcTextOnly += wordText
                        isFirstWord = false
                    }

                    val lineEndMs = lineStartMs + lineDuration
                    splLineBuilder.append("[").append(formatSplTime(lineEndMs)).append("]")

                    // 1. 写入带有时间戳的 YRC 逐字主歌词
                    sb.append(splLineBuilder.toString()).append("\n")
                    hasProcessedYrc = true

                    val isYrcMetadata = isMetadataLine(yrcTextOnly)

                    // 2. 防乱匹配
                    var bestMatchTime: Long? = null
                    val candidates = origMap.filterKeys { abs(it - lineStartMs) < 5000 }

                    if (candidates.isNotEmpty()) {
                        val normYrc = yrcTextOnly.replace(Regex("[^\\p{L}\\p{N}]"), "").lowercase()

                        // 级别1：纯文本完全一致
                        val exactMatch = candidates.entries.firstOrNull {
                            it.value.replace(Regex("[^\\p{L}\\p{N}]"), "").lowercase() == normYrc
                        }

                        if (exactMatch != null) {
                            bestMatchTime = exactMatch.key
                        } else {
                            // 级别2：包含子串匹配 (防YRC拆分句子)
                            val subMatch = candidates.entries.firstOrNull {
                                val normLrc = it.value.replace(Regex("[^\\p{L}\\p{N}]"), "").lowercase()
                                (normLrc.isNotBlank() && normYrc.isNotBlank()) &&
                                        (normLrc.contains(normYrc) || normYrc.contains(normLrc))
                            }
                            if (subMatch != null) {
                                bestMatchTime = subMatch.key
                            } else {
                                // 级别3：极严苛的时间匹配 (只允许1秒误差，防乱交)
                                val timeMatch = candidates.keys.minByOrNull { abs(it - lineStartMs) }
                                if (timeMatch != null && abs(timeMatch - lineStartMs) <= 1000) {
                                    bestMatchTime = timeMatch
                                }
                            }
                        }
                    }

                    // 3. 消费时间槽位并安全贴附翻译
                    if (bestMatchTime != null) {
                        // 如果不是属性标签，才赋予翻译！
                        if (!isYrcMetadata) {
                            if (useRoma && romaMap.containsKey(bestMatchTime)) {
                                sb.append(romaMap[bestMatchTime]).append("\n")
                            }
                            if (useTrans && transMap.containsKey(bestMatchTime)) {
                                sb.append(transMap[bestMatchTime]).append("\n")
                            }
                        }
                        // 只要配对成功，就坚决删除该时间槽，防止二次利用
                        romaMap.remove(bestMatchTime)
                        transMap.remove(bestMatchTime)
                        origMap.remove(bestMatchTime)
                    }
                }
            }

            // --- 降级处理 ---
            if (!hasProcessedYrc) {
                val allTimes = sortedSetOf<Long>()
                allTimes.addAll(origMap.keys)
                allTimes.addAll(romaMap.keys)
                allTimes.addAll(transMap.keys)

                if (allTimes.isEmpty()) return null

                for (time in allTimes) {
                    val timeTag = "[${formatSplTime(time)}]"
                    if (origMap.containsKey(time)) {
                        val text = origMap[time]!!
                        sb.append(timeTag).append(text).append("\n")
                        if (!isMetadataLine(text)) {
                            if (useRoma && romaMap.containsKey(time)) sb.append(romaMap[time]).append("\n")
                            if (useTrans && transMap.containsKey(time)) sb.append(transMap[time]).append("\n")
                        }
                    } else if (romaMap.containsKey(time)) {
                        sb.append(timeTag).append(romaMap[time]).append("\n")
                        if (useTrans && transMap.containsKey(time)) sb.append(transMap[time]).append("\n")
                    } else if (transMap.containsKey(time)) {
                        sb.append(timeTag).append(transMap[time]).append("\n")
                    }
                }
            }

            return sb.toString()
        }
    }
}