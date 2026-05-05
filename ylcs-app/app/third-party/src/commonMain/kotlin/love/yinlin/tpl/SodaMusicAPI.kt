package love.yinlin.tpl

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.tpl.lyrics.LrcParser
import love.yinlin.uri.Uri

@Stable
object SodaMusicAPI : PlatformMusicAPI {

    private const val SEARCH_API = "https://api.qishui.com/luna/pc/search/track"
    private const val PLAYLIST_API = "https://api.qishui.com/luna/pc/playlist/detail"
    private const val TRACK_SHARE_PAGE = "https://music.douyin.com/qishui/share/track"

    // 统一请求头，模拟真实浏览器，解决手机端无法解析的问题
    private val defaultHeaders = io.ktor.http.headers {
        append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        append("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        append("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        append("Origin", "https://music.douyin.com")
        append("Referer", "https://music.douyin.com/")
    }

    // ---------- 短链接解析 ----------
    private suspend fun extractPlaylistIdFromShortLink(shortUrl: String): String? {
        // 获取重定向后的最终 URL（不再解析 HTML）
        val finalUrl: String = NetClient.Common.request<String, String>({
            this.url = shortUrl
            headers = defaultHeaders
        }) {
            url
        } ?: return null

        // 直接从最终 URL 的参数中提取 playlist_id
        return Uri.parse(finalUrl)?.params?.get("playlist_id")
    }

    // ---------- 歌单 API ----------
    private suspend fun getPlaylistTrackIds(playlistId: String): List<String>? {
        val json: JsonObject = NetClient.Common.request<JsonObject>({
            url = "$PLAYLIST_API?playlist_id=$playlistId"
            headers = defaultHeaders
        }) { json: JsonObject -> json } ?: return null
        return json.arr("media_resources").mapNotNull { it.Object["id"]?.String }.ifEmpty { null }
    }

    // ---------- 封面 URL 构建 ----------
    private fun buildCoverUrl(albumInfo: JsonObject): String? {
        val urls = albumInfo.arr("urls").mapNotNull { it.String }
        val uri = albumInfo["uri"]?.String
        return if (urls.isNotEmpty() && uri != null) urls.first() + uri + "~c5_375x375.jpg" else null
    }

    // ---------- KRC 转 LRC ----------
    private fun convertKrcToLrc(content: String): String {
        val lines = content.split("\n")
        val lrcLines = mutableListOf<String>()
        val lineRegex = Regex("""\[(\d+),(\d+)](.*)""")
        val tagRegex = Regex("""<[^>]+>""")
        for (line in lines) {
            val match = lineRegex.find(line) ?: continue
            val startMs = match.groupValues[1].toLong()
            val textPart = match.groupValues[3]
            val pureText = tagRegex.replace(textPart, "")
            if (pureText.isNotBlank()) {
                val minutes = startMs / 60000
                val seconds = (startMs % 60000) / 1000
                val centi = (startMs % 1000) / 10
                val timeStr = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${centi.toString().padStart(2, '0')}"
                lrcLines.add("[$timeStr]$pureText")
            }
        }
        return lrcLines.joinToString("\n")
    }

    // ---------- 获取完整歌单（单首歌失败不影响其他）----------
    private suspend fun fetchPlaylist(playlistId: String): List<PlatformMusicInfo>? {
        val trackIds = getPlaylistTrackIds(playlistId) ?: return null
        val result = mutableListOf<PlatformMusicInfo>()
        for (trackId in trackIds) {
            try {
                fetchTrackInfo(trackId)?.let { result.add(it) }
            } catch (e: Exception) {
                // 单首歌曲解析失败，不影响其他歌曲
                println("SodaMusicAPI: 解析歌曲 $trackId 失败: ${e.message}")
            }
        }
        return result.ifEmpty { null }
    }

    // ---------- 获取单曲信息（最外层捕获所有异常，返回 null）----------
    private suspend fun fetchTrackInfo(trackId: String): PlatformMusicInfo? {
        return try {
            // 1. 获取音频链接（从分享页）
            var audioUrl = ""
            val html = NetClient.Common.request<String>({
                url = "$TRACK_SHARE_PAGE?track_id=$trackId"
                headers = defaultHeaders
            }) { text: String -> text }
            if (html != null) {
                val regex = Regex("""(?s)_ROUTER_DATA\s*=\s*(\{.*?\});""")
                regex.find(html)?.let { match ->
                    val jsonString = match.groupValues[1].replace("\\u002F", "/")
                    val routerData = Json.decodeFromString<JsonObject>(jsonString)
                    val rawUrl = routerData.obj("loaderData")
                        .obj("track_page")
                        .obj("audioWithLyricsOption")["url"]?.String
                    if (rawUrl != null) {
                        audioUrl = rawUrl.replace("\\u002F", "/")
                    }
                }
            }

            // 2. 音频格式过滤：仅丢弃 .mp4 视频，其他所有格式都保留
            if (audioUrl.isNotEmpty() && audioUrl.lowercase().endsWith(".mp4")) {
                return null  // 丢弃整首视频歌曲
            }

            // 3. 元数据 API
            val apiData: JsonObject = NetClient.Common.request<JsonObject>({
                url = "https://api.qishui.com/luna/pc/track_v2?track_id=$trackId&media_type=track"
                headers = defaultHeaders
            }) { json: JsonObject -> json } ?: return null

            val trackObj = apiData.obj("track")
            val name = trackObj["name"]?.String ?: ""
            val artists = trackObj.arr("artists").mapNotNull { it.Object["name"]?.String }
            val singer = artists.joinToString("、")
            val durationMs = trackObj["duration"]?.Long ?: 0L
            val cover = buildCoverUrl(trackObj.obj("album").obj("url_cover")) ?: ""

            val krcContent = apiData.obj("lyric")["content"]?.String ?: ""
            val lyrics = if (krcContent.isNotBlank()) {
                val lrcText = convertKrcToLrc(krcContent)
                LrcParser(lrcText).toString()
            } else ""

            PlatformMusicInfo(
                id = trackId,
                name = name,
                singer = singer,
                time = durationMs.timeString,
                pic = cover,
                audioUrl = audioUrl,
                lyrics = lyrics
            )
        } catch (ignored: Exception) {
            // 任何异常都导致该歌曲解析失败，返回 null
            null
        }
    }

    // ---------- 搜索 ----------
    override suspend fun search(keyword: String): List<PlatformMusicInfo>? {
        val json: JsonObject = NetClient.Common.request<JsonObject>({
            url = "$SEARCH_API?aid=386088&q=${Uri.encodeUri(keyword)}"
            headers = defaultHeaders
        }) { json: JsonObject -> json } ?: return null

        val trackIds = json.arr("result_groups").firstOrNull()?.Object
            ?.arr("data")?.mapNotNull { item ->
                item.Object.obj("entity").obj("track")["id"]?.String
            } ?: emptyList()

        if (trackIds.isEmpty()) return null
        val result = mutableListOf<PlatformMusicInfo>()
        for (tid in trackIds) {
            try {
                fetchTrackInfo(tid)?.let { result.add(it) }
            } catch (e: Exception) {
                // 忽略单曲错误
            }
        }
        return result.ifEmpty { null }
    }

    // ---------- 解析链接 ----------
    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = Coroutines.io {
        when {
            // 短链接（歌单）-> 例如 https://qishui.douyin.com/s/ix7xxWf3/
            link.contains("qishui.douyin.com/s/") -> {
                val id = extractPlaylistIdFromShortLink(link)
                if (id != null) fetchPlaylist(id) else null
            }
            // 长链接 playlist_id -> 例如 https://music.douyin.com/qishui/share/playlist?playlist_id=7635277649740922923&...
            link.contains("playlist_id=") -> {
                val id = """playlist_id=(\d+)""".toRegex().find(link)?.groupValues?.get(1)
                if (id != null) fetchPlaylist(id) else null
            }
            // 长链接 track_id -> 例如 https://music.douyin.com/qishui/share/track?track_id=7145746290255595521
            link.contains("track_id=") -> {
                val id = """track_id=(\d+)""".toRegex().find(link)?.groupValues?.get(1)
                if (id != null) fetchTrackInfo(id)?.let { listOf(it) } else null
            }
            // 纯数字作为单曲 ID -> 例如 7145746290255595521
            link.matches(Regex("\\d+")) -> {
                fetchTrackInfo(link)?.let { listOf(it) }
            }
            // 默认按搜索处理
            else -> search(link)
        }
    }
}