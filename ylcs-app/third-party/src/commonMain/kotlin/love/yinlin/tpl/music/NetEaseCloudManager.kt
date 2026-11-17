package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.music.NetEaseCloudInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.tpl.music.PlatformMusicRule.Kind
import love.yinlin.uri.Uri

@Stable
object NetEaseCloudManager : PlatformMusicManager(PlatformMusicType.NetEaseCloud) {
    private val shortTrack = Regex("""https://music\.163\.com/song\?id=(\d+)""")
    private const val HOST = "music.163.com"
    private const val SHORT_HOST = "163cn.tv"

    override val rules: List<PlatformMusicRule> = [
        // 歌曲 http://163cn.tv/EElG0jr
        PlatformMusicRule.redirect("短链接", SHORT_HOST),
        // 歌单 https://y.music.163.com/m/playlist?id=13674538430&userid=10015279209&creatorId=10015279209
        PlatformMusicRule.link("歌单", Kind.Playlist, HOST) { uri ->
            uri.parameterId("id")?.takeIf { id ->
                uri.path.orEmpty().contains("playlist") && numericTrackId.matches(id)
            }
        },
        // 歌曲 https://music.163.com/#/song?textid=1064008&id=504686858
        PlatformMusicRule.link("单曲", Kind.Track, HOST) { uri ->
            uri.parameterId("id")?.takeIf { id ->
                uri.path.orEmpty().contains("song") && numericTrackId.matches(id)
            }
        },
        // 歌曲 504686858
        PlatformMusicRule.trackId("单曲ID", Regex("[0-9]{1,18}")),
    ]

    private fun summary(json: JsonObject): PlatformMusicInfo? {
        val id = json["id"].StringNull?.takeIf(numericTrackId::matches) ?: return null
        val name = json["name"].StringNull ?: return null
        val artists = json["ar"].ArrayNull ?: json["artists"].ArrayNull.orEmpty()
        val album = json["al"].ObjectNull ?: json["album"].ObjectEmpty
        return NetEaseCloudInfo(id, name, artists.mapNotNull { it.ObjectNull?.get("name").StringNull }.joinToString("、"), album["picUrl"].StringNull.orEmpty())
    }

    private suspend fun requestTrack(id: String): PlatformMusicInfo? = NetClient.Common.request({
        url = "https://$HOST/api/song/detail?id=$id&ids=[$id]"
    }) { json: JsonObject ->
        json.arr("songs").firstOrNull()?.ObjectNull?.let(::summary) ?: error("未找到歌曲")
    }

    private suspend fun requestPlaylist(id: String): List<PlatformMusicInfo>? = NetClient.Common.request({
        url = "https://$HOST/api/playlist/detail?id=$id&offset=0&limit=1000"
    }) { json: JsonObject ->
        json.obj("result").arr("tracks").mapNotNull { it.ObjectNull?.let(::summary) }
    }

    override suspend fun searchKeyword(keyword: String, results: MutableList<PlatformMusicInfo>) {
        val tracks = NetClient.Common.request({
            url = "https://$HOST/api/cloudsearch/pc?s=${Uri.encodeUri(keyword)}&type=1&limit=50&offset=0"
        }) { json: JsonObject ->
            val result = json.obj("result")
            val songs = result["songs"].ArrayNull
            if (songs == null) check(result["songCount"].Long == 0L)
            songs.orEmpty().mapNotNull { it.ObjectNull?.let(::summary) }
        }
        results.addBatch(tracks, "搜索结果")
    }

    override suspend fun findTrack(id: String): PlatformMusicInfo? = requestTrack(id)

    override suspend fun findPlaylist(id: String, results: MutableList<PlatformMusicInfo>) {
        results.addBatch(requestPlaylist(id), "歌单")
    }

    override suspend fun resolveRedirect(link: String): Uri? = NetClient.Common.request<String, Uri>({
        url = link
    }) {
        val target = Uri.parse(url)
        if (target != null && PlatformMusicRule.isMusicLinkOn(target, HOST) && target.parameterId("id") != null) target
        else shortTrack.find(bodyString)?.value?.let(Uri::parse) ?: error("无法解析音乐短链接")
    }

    override suspend fun resolveDownload(track: PlatformMusicInfo): Download? {
        if (!numericTrackId.matches(track.id)) return null
        val cover = track.pic.ifEmpty { requestTrack(track.id)?.pic.orEmpty() }
        val lyrics = NetClient.Common.request({
            url = "https://$HOST/api/song/lyric?id=${track.id}&lv=1"
        }) { json: JsonObject ->
            json["lrc"].ObjectNull?.get("lyric").StringNull.orEmpty()
        }
        return Download("https://$HOST/song/media/outer/url?id=${track.id}", cover, lyrics.orEmpty())
    }
}
