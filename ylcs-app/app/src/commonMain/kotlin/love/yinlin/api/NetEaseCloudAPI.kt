package love.yinlin.api

import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.Data
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.extension.*
import love.yinlin.platform.NetClient
import love.yinlin.platform.app
import love.yinlin.platform.safeGet
import love.yinlin.ui.component.lyrics.LyricsLrc

object NetEaseCloudAPI {
    private const val NETEASECLOUD_HOST: String = "music.163.com"
    private object Container {
        fun detail(id: String) = "api/song/detail?id=${id}&ids=[${id}]"
        fun playlist(id: String) = "api/playlist/detail?id=$id&offset=0&limit=1000"
        fun lyrics(id: String) = "api/song/lyric?id=${id}&lv=1"
        fun mp3(id: String) = "song/media/outer/url?id=${id}"
    }

    suspend fun requestMusicId(url: String): Data<String> = NetClient.common.safeGet(url) { body: ByteArray ->
        "https://music\\.163\\.com/song\\?id=(\\d+)".toRegex().find(body.decodeToString())!!.groupValues[1]
    }

    private fun getCloudMusic(json: JsonObject): PlatformMusicInfo = PlatformMusicInfo(
        id = json["id"].String,
        name = json["name"].String,
        singer = json.arr("artists").fastJoinToString(",") { it.Object["name"].String },
        time = json["duration"].Long.timeString,
        pic = json.obj("album")["picUrl"].String,
        audioUrl = "https://$NETEASECLOUD_HOST/${Container.mp3(json["id"].String)}",
        lyrics = ""
    )

    private suspend fun requestLyrics(id: String): Data<String> = NetClient.common.safeGet(
        url = "https://$NETEASECLOUD_HOST/${Container.lyrics(id)}"
    ) { json: JsonObject ->
        val text = json.obj("lrc")["lyric"].String
        LyricsLrc.Parser(text).toString()
    }

    suspend fun requestMusic(id: String): Data<PlatformMusicInfo> {
        val result1 = NetClient.common.safeGet(
            url = "https://$NETEASECLOUD_HOST/${Container.detail(id)}"
        ) { json: JsonObject ->
            getCloudMusic(json.arr("songs")[0].Object)
        }
        return when (result1) {
            is Data.Success -> when (val result2 = requestLyrics(id)) {
                is Data.Success -> Data.Success(result1.data.copy(lyrics = result2.data))
                is Data.Failure -> Data.Failure()
            }
            is Data.Failure -> result1
        }
    }

    suspend fun requestPlaylist(id: String): Data<List<PlatformMusicInfo>> {
        val result1 = NetClient.common.safeGet(
            url = "https://$NETEASECLOUD_HOST/${Container.playlist(id)}"
        ) { json: JsonObject ->
            json.obj("result").arr("tracks").fastMap {
                getCloudMusic(it.Object)
            }
        }
        return when (result1) {
            is Data.Success -> {
                val data = result1.data.toMutableList()
                for (i in data.indices) {
                    val result2 = requestLyrics(data[i].id)
                    if (result2 is Data.Success) data[i] = data[i].copy(lyrics = result2.data)
                }
                data.removeAll { it.lyrics.isEmpty() }
                if (data.isEmpty()) Data.Failure() else Data.Success(data)
            }
            is Data.Failure -> result1
        }
    }
}