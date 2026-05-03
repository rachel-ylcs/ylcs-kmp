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

    // ---------- 短链接解析 ----------
    private suspend fun extractPlaylistIdFromShortLink(shortUrl: String): String? {
        val html = NetClient.Common.request<String>({
            this.url = shortUrl
            headers = io.ktor.http.headers {
                append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                append("Referer", "https://music.douyin.com/")
            }
        }) { text: String -> text } ?: return null

        val metaRegex = Regex("""<meta[^>]*?name="url"[^>]*?content="([^">]+)"""")
        val longUrl = metaRegex.find(html)?.groupValues?.get(1) ?: return null
        return """playlist_id=(\d+)""".toRegex().find(longUrl)?.groupValues?.get(1)
    }

    // ---------- 歌单 API ----------
    private suspend fun getPlaylistTrackIds(playlistId: String): List<String>? {
        val json: JsonObject = NetClient.Common.request<JsonObject>({
            url = "$PLAYLIST_API?playlist_id=$playlistId"
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

    // ---------- 获取完整歌单 ----------
    private suspend fun fetchPlaylist(playlistId: String): List<PlatformMusicInfo>? {
        val trackIds = getPlaylistTrackIds(playlistId) ?: return null
        val result = mutableListOf<PlatformMusicInfo>()
        for (trackId in trackIds) {
            fetchTrackInfo(trackId)?.let { result.add(it) }
        }
        return result.ifEmpty { null }
    }

    // ---------- 获取单曲信息 ----------
    private suspend fun fetchTrackInfo(trackId: String): PlatformMusicInfo? {
        // 音频链接
        var audioUrl = ""
        val html = NetClient.Common.request<String>({
            url = "$TRACK_SHARE_PAGE?track_id=$trackId"
            headers = io.ktor.http.headers {
                append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                append("Referer", "https://music.douyin.com/")
            }
        }) { text: String -> text }
        if (html != null) {
            try {
                val regex = Regex("""(?s)_ROUTER_DATA\s*=\s*(\{.*?\});""")
                regex.find(html)?.let { match ->
                    val jsonString = match.groupValues[1].replace("\\u002F", "/")
                    val routerData = Json.decodeFromString<JsonObject>(jsonString)
                    val rawUrl = routerData.obj("loaderData")
                        .obj("track_page")
                        .obj("audioWithLyricsOption")["url"]?.String
                    if (rawUrl != null) audioUrl = rawUrl.replace("\\u002F", "/")
                }
            } catch (ignored: Exception) {}
        }

        // 元数据
        val apiData: JsonObject = NetClient.Common.request<JsonObject>({
            url = "https://api.qishui.com/luna/pc/track_v2?track_id=$trackId&media_type=track"
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

        return PlatformMusicInfo(
            id = trackId,
            name = name,
            singer = singer,
            time = durationMs.timeString,
            pic = cover,
            audioUrl = audioUrl,
            lyrics = lyrics
        )
    }

    // ---------- 搜索 ----------
    override suspend fun search(keyword: String): List<PlatformMusicInfo>? {
        val json: JsonObject = NetClient.Common.request<JsonObject>({
            url = "$SEARCH_API?aid=386088&q=${Uri.encodeUri(keyword)}"
        }) { json: JsonObject -> json } ?: return null

        val trackIds = json.arr("result_groups").firstOrNull()?.Object
            ?.arr("data")?.mapNotNull { item ->
                item.Object.obj("entity").obj("track")["id"]?.String
            } ?: emptyList()

        if (trackIds.isEmpty()) return null
        val result = mutableListOf<PlatformMusicInfo>()
        for (tid in trackIds) {
            fetchTrackInfo(tid)?.let { result.add(it) }
        }
        return result.ifEmpty { null }
    }

    // ---------- 解析链接 ----------
    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = Coroutines.io {
        var result: List<PlatformMusicInfo>? = null
        if (link.contains("qishui.douyin.com/s/")) {
            val id = extractPlaylistIdFromShortLink(link)
            if (id != null) {
                result = fetchPlaylist(id)
            }
        } else if (link.contains("playlist_id=")) {
            val id = """playlist_id=(\d+)""".toRegex().find(link)?.groupValues?.get(1)
            if (id != null) {
                result = fetchPlaylist(id)
            }
        } else if (link.contains("track_id=")) {
            val id = """track_id=(\d+)""".toRegex().find(link)?.groupValues?.get(1)
            if (id != null) {
                val info = fetchTrackInfo(id)
                result = if (info != null) listOf(info) else null
            }
        } else if (link.matches(Regex("\\d+"))) {
            val info = fetchTrackInfo(link)
            result = if (info != null) listOf(info) else null
        }
        result
    }
}