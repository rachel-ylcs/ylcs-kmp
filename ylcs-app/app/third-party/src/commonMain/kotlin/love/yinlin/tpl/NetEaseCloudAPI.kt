package love.yinlin.tpl

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonObject
import love.yinlin.coroutines.Coroutines
import love.yinlin.cs.NetClient
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.extension.*
import love.yinlin.tpl.lyrics.LrcParser
import love.yinlin.uri.Uri

@Stable
object NetEaseCloudAPI : PlatformMusicParser {
    private const val NETEASECLOUD_HOST: String = "music.163.com"
    private object Container {
        fun detail(id: String) = "api/song/detail?id=${id}&ids=[${id}]"
        fun playlist(id: String) = "api/playlist/detail?id=$id&offset=0&limit=1000"
        fun lyrics(id: String) = "api/song/lyric?id=${id}&lv=1"
        fun mp3(id: String) = "song/media/outer/url?id=${id}"
    }

    suspend fun requestMusicId(url: String): String? = NetClient.request(url) { text: String ->
        "https://music\\.163\\.com/song\\?id=(\\d+)".toRegex().find(text)!!.groupValues[1]
    }

    private fun getCloudMusic(json: JsonObject): PlatformMusicInfo = PlatformMusicInfo(
        id = json["id"].String,
        name = json["name"].String,
        singer = json.arr("artists").joinToString(",") { it.Object["name"].String },
        time = json["duration"].Long.timeString,
        pic = json.obj("album")["picUrl"].String,
        audioUrl = "https://$NETEASECLOUD_HOST/${Container.mp3(json["id"].String)}",
        lyrics = ""
    )

    private suspend fun requestLyrics(id: String): String? = NetClient.request("https://$NETEASECLOUD_HOST/${Container.lyrics(id)}") { json: JsonObject ->
        val text = json.obj("lrc")["lyric"].String
        LrcParser(text).toString()
    }

    suspend fun requestMusic(id: String): PlatformMusicInfo? = NetClient.request("https://$NETEASECLOUD_HOST/${Container.detail(id)}") { json: JsonObject ->
        getCloudMusic(json.arr("songs")[0].Object)
    }?.let { musicInfo ->
        requestLyrics(id)?.let { musicInfo.copy(lyrics = it) }
    }

    suspend fun requestPlaylist(id: String): List<PlatformMusicInfo>? = NetClient.request("https://$NETEASECLOUD_HOST/${Container.playlist(id)}") { json: JsonObject ->
        json.obj("result").arr("tracks").map {
            getCloudMusic(it.Object)
        }.toMutableList()
    }?.let { musicInfos ->
        musicInfos.forEachIndexed { i, info ->
            requestLyrics(info.id)?.let { musicInfos[i] = info.copy(lyrics = it) }
        }
        musicInfos.removeAll { it.lyrics.isEmpty() }
        musicInfos.ifEmpty { null }
    }

    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = when {
        // 歌曲 http://163cn.tv/EElG0jr
        link.contains("163cn.tv") -> Coroutines.io {
            requestMusicId(link)?.let { requestMusic(it) }
        }?.let { listOf(it) }
        // 歌单 https://y.music.163.com/m/playlist?id=13674538430&userid=10015279209&creatorId=10015279209
        link.contains("music.163.com") && link.contains("playlist") -> Coroutines.io {
            val id = Uri.parse(link)?.params["id"]
            if (id != null) requestPlaylist(id) else null
        }
        // 歌曲 https://music.163.com/#/song?textid=1064008&id=504686858
        link.contains("music.163.com") && link.contains("song") -> Coroutines.io {
            val id = Uri.parse(link)?.params["id"]
            if (id != null) requestMusic(id) else null
        }?.let { listOf(it) }
        // 歌曲 504686858
        else -> Coroutines.io { requestMusic(link) }?.let { listOf(it) }
    }
}