package love.yinlin.api

import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.json.JsonObject
import love.yinlin.uri.Uri
import love.yinlin.data.Data
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.extension.JsonObjectScope
import love.yinlin.extension.Long
import love.yinlin.extension.Object
import love.yinlin.extension.String
import love.yinlin.extension.arr
import love.yinlin.extension.makeObject
import love.yinlin.extension.obj
import love.yinlin.extension.parseJson
import love.yinlin.extension.timeString
import love.yinlin.extension.toJsonString
import love.yinlin.platform.NetClient
import love.yinlin.platform.safeGet
import love.yinlin.ui.component.lyrics.LyricsLrc
import kotlin.io.encoding.Base64

object QQMusicAPI {
    private inline fun buildUrl(data: JsonObjectScope.() -> Unit): String = "https://u.y.qq.com/cgi-bin/musicu.fcg?data=${Uri.encodeUri(makeObject(data).toJsonString())}"

    private fun decodeData(num: Int, body: ByteArray): List<JsonObject> {
        val json = body.decodeToString().parseJson.Object
        val arr = mutableListOf<JsonObject>()
        repeat(num) {
            arr += json.obj("req_$it").obj("data")
        }
        return arr
    }

    suspend fun requestMusicId(url: String): Data<String> = NetClient.common.safeGet(url) { body: ByteArray ->
        "\"mid\":\\s*\"([^\"]*)".toRegex().find(body.decodeToString())!!.groupValues[1]
    }

    suspend fun requestMusic(id: String): Data<PlatformMusicInfo> = NetClient.common.safeGet(buildUrl {
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
    }) { body: ByteArray ->
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
            lyrics = LyricsLrc.Parser(Base64.decode(lyricsBase64).decodeToString()).toString()
        )
    }

    suspend fun requestPlaylist(id: String): Data<List<PlatformMusicInfo>> {
        val result1 = NetClient.common.safeGet(buildUrl {
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
        }) { body: ByteArray ->
            val (json) = decodeData(1, body)
            json.arr("songlist").fastMap { it.Object["mid"].String }
        }
        return when (result1) {
            is Data.Success -> {
                val items = mutableListOf<PlatformMusicInfo>()
                for (mid in result1.data) {
                    val result2 = requestMusic(mid)
                    if (result2 is Data.Success) items += result2.data
                }
                if (items.isEmpty()) Data.Failure() else Data.Success(items)
            }
            is Data.Failure -> result1
        }
    }
}