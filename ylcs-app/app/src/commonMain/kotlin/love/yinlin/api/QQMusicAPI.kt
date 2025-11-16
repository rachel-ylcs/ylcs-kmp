package love.yinlin.api

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.json.JsonObject
import love.yinlin.platform.lyrics.LrcParser
import love.yinlin.uri.Uri
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.extension.*
import love.yinlin.platform.NetClient
import kotlin.io.encoding.Base64

@Stable
data object QQMusicAPI {
    private inline fun buildUrl(data: JsonObjectScope.() -> Unit): String = "https://u.y.qq.com/cgi-bin/musicu.fcg?data=${Uri.encodeUri(makeObject(data).toJsonString())}"

    private fun decodeData(num: Int, json: JsonObject): List<JsonObject> {
        val arr = mutableListOf<JsonObject>()
        repeat(num) {
            arr += json.obj("req_$it").obj("data")
        }
        return arr
    }

    suspend fun requestMusicId(url: String): String? = NetClient.request(url) { text: String ->
        "\"mid\":\\s*\"([^\"]*)".toRegex().find(text)!!.groupValues[1]
    }

    suspend fun requestMusic(id: String): PlatformMusicInfo? = NetClient.request(buildUrl {
        obj("req_0") {
            "module" with "music.pf_song_detail_svr"
            "method" with "get_song_detail_yqq"
            obj("param") { "song_mid" with id }
        }
        obj("req_1") {
            "module" with "music.musichallSong.PlayLyricInfo"
            "method" with "GetPlayLyricInfo"
            obj("param") { "songMID" with id }
        }
        obj("req_2") {
            "module" with "vkey.GetVkeyServer"
            "method" with "CgiGetVkey"
            obj("param") {
                arr("filename") { add("C400$id$id.m4a") }
                arr("songmid") { add(id) }
                arr("songtype") { add(0) }
                "guid" with "19911211"
            }
        }
    }) { body: JsonObject ->
        val (json1, json2, json3) = decodeData(3, body)
        val trackInfo = json1.obj("track_info")
        val lyricsBase64 = json2["lyric"].String
        val midUrlInfo = json3.arr("midurlinfo")[0].Object
        PlatformMusicInfo(
            id = trackInfo["mid"].String,
            name = trackInfo["name"].String,
            singer = trackInfo.arr("singer").fastJoinToString(",") { it.Object["name"].String },
            time = (trackInfo["interval"].Long * 1000).timeString,
            pic = "https://y.qq.com/music/photo_new/T002R300x300M000${trackInfo.obj("album")["pmid"].String}.jpg?max_age=2592000",
            audioUrl = "https://ws.stream.qqmusic.qq.com/${midUrlInfo["purl"].String}",
            lyrics = LrcParser(Base64.decode(lyricsBase64).decodeToString()).toString()
        )
    }

    suspend fun requestPlaylist(id: String): List<PlatformMusicInfo>? = NetClient.request(buildUrl {
        obj("req_0") {
            "module" with "music.srfDissInfo.aiDissInfo"
            "method" with "uniform_get_Dissinfo"
            obj("param") {
                "disstid" with (id.toLongOrNull() ?: 0L)
                "orderlist" with 1
                "song_begin" with 0
                "song_num" with 1000
            }
        }
    }) { body: JsonObject ->
        val (json) = decodeData(1, body)
        json.arr("songlist").fastMap { it.Object["mid"].String }
    }?.let { list ->
        val items = mutableListOf<PlatformMusicInfo>()
        for (mid in list) requestMusic(mid)?.let { items += it }
        items.ifEmpty { null }
    }
}