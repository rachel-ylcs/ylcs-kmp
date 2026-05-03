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

    private suspend fun extractPlaylistIdFromShortLink(url: String): String? =
        NetClient.Common.request<String>({
            this.url = url
        }) { text: String ->
            """playlist_id=(\d+)""".toRegex().find(text)?.groupValues?.get(1) ?: ""
        }?.takeIf { it.isNotEmpty() }

    private suspend fun getPlaylistTrackIds(playlistId: String): List<String>? {
        val json: JsonObject = NetClient.Common.request<JsonObject>({
            url = "$PLAYLIST_API?playlist_id=$playlistId"
        }) { json: JsonObject -> json } ?: return null
        return json.arr("media_resources").mapNotNull { it.Object["id"]?.String }.ifEmpty { null }
    }

    private suspend fun fetchTrackInfo(trackId: String): PlatformMusicInfo? {
        val apiData: JsonObject = NetClient.Common.request<JsonObject>({
            url = "https://api.qishui.com/luna/pc/track_v2?track_id=$trackId&media_type=track"
        }) { json: JsonObject -> json } ?: return null

        val trackObj = apiData.obj("track")
        val name = trackObj["name"].String!!
        val singer = trackObj.arr("artists").joinToString("、") { it.Object["name"].String!! }
        val album = trackObj.obj("album")["name"].String!!
        val durationMs = trackObj["duration"].Long!!

        val songMaker = trackObj.obj("song_maker_team")
        val composer = songMaker.arr("composers").mapNotNull { it.Object["name"]?.String }.joinToString("、")
        val lyricist = songMaker.arr("lyricists").mapNotNull { it.Object["name"]?.String }.joinToString("、")

        val albumCoverInfo = trackObj.obj("album").obj("url_cover")
        val cover = albumCoverInfo.arr("urls").first().String!! + albumCoverInfo["uri"].String!! + "~c5_375x375.jpg"

        val lyricObj = apiData.obj("lyric")
        val krcContent = lyricObj["content"]?.String ?: ""
        val lyrics = if (krcContent.isNotBlank()) {
            val lines = krcContent.split("\n")
            val lrcLines = mutableListOf<String>()
            val lineRegex = Regex("""\[(\d+),(\d+)\](.*)""")
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
            LrcParser(lrcLines.joinToString("\n")).toString()
        } else ""

        val html: String = NetClient.Common.request<String>({
            this.url = "$TRACK_SHARE_PAGE?track_id=$trackId"
        }) { text: String -> text } ?: return null

        val regex = Regex("""_ROUTER_DATA\s*=\s*(\{.*?\});""", RegexOption.DOT_MATCHES_ALL)
        val jsonString = regex.find(html)?.groupValues?.get(1) ?: return null
        val cleanJson = jsonString.replace("\\u002F", "/")
        val routerData = Json.decodeFromString<JsonObject>(cleanJson)

        val trackPage = routerData.obj("loaderData").obj("track_page")
        val audioOption = trackPage.obj("audioWithLyricsOption")
        val rawUrl = audioOption["url"].String!!
        val audioUrl = rawUrl.replace("\\u002F", "/")

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

    override suspend fun search(keyword: String): List<PlatformMusicInfo>? {
        val json: JsonObject = NetClient.Common.request<JsonObject>({
            url = "$SEARCH_API?aid=386088&q=${Uri.encodeUri(keyword)}"
        }) { json: JsonObject -> json } ?: return null
        val trackIds = json.arr("result_groups").firstOrNull()?.Object
            ?.arr("data")?.mapNotNull { it.Object.obj("entity").obj("track")["id"]?.String } ?: emptyList()
        if (trackIds.isEmpty()) return null
        return trackIds.mapNotNull { fetchTrackInfo(it) }.ifEmpty { null }
    }

    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = Coroutines.io {
        if (link.contains("qishui.douyin.com/s/")) {
            extractPlaylistIdFromShortLink(link)?.let { playlistId ->
                getPlaylistTrackIds(playlistId)?.let { trackIds ->
                    return@io trackIds.mapNotNull { fetchTrackInfo(it) }.ifEmpty { null }
                }
            }
        }
        """playlist_id=(\d+)""".toRegex().find(link)?.groupValues?.get(1)?.let { playlistId ->
            getPlaylistTrackIds(playlistId)?.let { trackIds ->
                return@io trackIds.mapNotNull { fetchTrackInfo(it) }.ifEmpty { null }
            }
        }
        """track_id=(\d+)""".toRegex().find(link)?.groupValues?.get(1)?.let { trackId ->
            fetchTrackInfo(trackId)?.let { return@io listOf(it) }
        }
        search(link)
    }
}