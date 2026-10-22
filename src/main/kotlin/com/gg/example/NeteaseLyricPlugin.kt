@file:OptIn(UnstableSpwWorkshopApi::class)

package com.gg.example

import com.gg.example.lyrics.LyricMerger
import com.gg.example.lyrics.LyricOptions
import com.gg.example.network.NeteaseClient
import com.gg.example.network.SearchKeyword
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint
import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi
import com.xuncorp.spw.workshop.api.WorkshopApi
import org.pf4j.Extension
import org.pf4j.Plugin
import org.pf4j.PluginWrapper

class NeteaseLyricPlugin(wrapper: PluginWrapper) : Plugin(wrapper) {
    @Extension
    class LyricProvider : PlaybackExtensionPoint {
        private val client = NeteaseClient()
        private val merger = LyricMerger()
        private val configManager = WorkshopApi.manager.createConfigManager()
        private val configHelper = configManager.getConfig()

        override fun onAfterLoadLyrics(mediaItem: PlaybackExtensionPoint.MediaItem): String? {
            configHelper.reload()
            if (!configHelper.get("enable_plugin", true)) return null

            val showToast = configHelper.get("show_toast", false)
            fun toast(message: String, type: WorkshopApi.Ui.ToastType) {
                if (showToast) WorkshopApi.ui.toast(message, type)
            }

            val keyword = "${mediaItem.title} ${mediaItem.artist}".trim()
            if (keyword.isBlank()) return null
            toast("正在匹配: ${mediaItem.title}", WorkshopApi.Ui.ToastType.Success)

            try {
                var songId = client.searchSongId(keyword)
                if (songId == null) {
                    val cleaned = SearchKeyword.clean(keyword)
                    if (cleaned.isNotBlank() && cleaned != keyword) {
                        songId = client.searchSongId(cleaned)
                    }
                }
                if (songId == null) {
                    toast("未找到歌曲: ${mediaItem.title}", WorkshopApi.Ui.ToastType.Warning)
                    return null
                }

                val payload = client.fetchLyrics(songId) ?: return null
                configHelper.reload()
                val options = LyricOptions(
                    translation = configHelper.get("enable_translation", true),
                    romanization = configHelper.get("enable_roman", false),
                    wordSynced = configHelper.get("enable_yrc", true),
                )
                val lyrics = merger.merge(payload, options)
                if (!lyrics.isNullOrBlank()) {
                    toast("匹配成功!", WorkshopApi.Ui.ToastType.Success)
                    return lyrics
                }
            } catch (exception: Exception) {
                toast("匹配出错: ${exception.message}", WorkshopApi.Ui.ToastType.Error)
                exception.printStackTrace()
            }
            return null
        }
    }
}
