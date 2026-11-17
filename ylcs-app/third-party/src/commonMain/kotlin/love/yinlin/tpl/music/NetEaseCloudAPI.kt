package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.parser.lrc.Lrc
import love.yinlin.uri.Uri

@Stable
object NetEaseCloudAPI : PlatformMusicAPI {
    private const val HOST = "music.163.com"
    private const val SHORT_HOST = "163cn.tv"

    override val type = PlatformMusicType.NetEaseCloud

    override fun matchesLink(uri: Uri): Boolean = uri.isMusicLinkOn(HOST) || uri.isMusicLinkOn(SHORT_HOST)
    override fun matchesTrackId(id: String): Boolean = id.length <= 18 && numericTrackId.matches(id)

    private fun summary(json: JsonObject): PlatformMusicInfo? {
        val id = json["id"].StringNull?.takeIf(numericTrackId::matches) ?: return null
        val name = json["name"].StringNull ?: return null
        val artists = json["ar"].ArrayNull ?: json["artists"].ArrayNull.orEmpty()
        val album = json["al"].ObjectNull ?: json["album"].ObjectEmpty
        return PlatformMusicInfo(id, name, artists.mapNotNull { it.ObjectNull?.get("name").StringNull }.joinToString("、"), album["picUrl"].StringNull.orEmpty())
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

    override fun search(keyword: String) = flow {
        val tracks = NetClient.Common.request({
            url = "https://$HOST/api/cloudsearch/pc?s=${Uri.encodeUri(keyword)}&type=1&limit=50&offset=0"
        }) { json: JsonObject ->
            val result = json.obj("result")
            val songs = result["songs"].ArrayNull
            if (songs == null) check(result["songCount"].Long == 0L)
            songs.orEmpty().mapNotNull { it.ObjectNull?.let(::summary) }
        }
        emit(checkNotNull(tracks) { "网易云音乐搜索失败" })
    }

    override fun findTrack(id: String) = flow {
        require(numericTrackId.matches(id)) { "歌曲 ID 无效" }
        emit([checkNotNull(requestTrack(id)) { "未找到网易云音乐歌曲" }])
    }

    override fun parseLink(uri: Uri) = flow {
        require(matchesLink(uri)) { "请输入网易云音乐链接" }
        val target = if (uri.isMusicLinkOn(SHORT_HOST)) {
            NetClient.Common.request<String, Uri>({
                url = uri.toString()
            }) {
                Uri.parse(url)?.takeIf { it.isMusicLinkOn(HOST) && it.parameterId("id") != null }
                    ?: shortTrack.find(bodyString)?.value?.let(Uri::parse) ?: error("无法解析短链接")
            } ?: error("无法解析网易云音乐短链接")
        } else uri
        val id = target.parameterId("id")?.takeIf(numericTrackId::matches) ?: error("链接缺少有效 ID")
        if (target.path.orEmpty().contains("playlist")) emit(checkNotNull(requestPlaylist(id)) { "读取网易云音乐歌单失败" })
        else findTrack(id).collect { emit(it) }
    }

    override suspend fun resolveDownload(track: PlatformMusicInfo): PlatformMusicResources? {
        if (!numericTrackId.matches(track.id)) return null
        val cover = track.pic.ifEmpty { requestTrack(track.id)?.pic.orEmpty() }
        val lyrics = NetClient.Common.request({
            url = "https://$HOST/api/song/lyric?id=${track.id}&lv=1"
        }) { json: JsonObject ->
            json["lrc"].ObjectNull?.get("lyric").StringNull?.let { Lrc.parse(it).toString() }.orEmpty()
        }
        return PlatformMusicResources("https://$HOST/song/media/outer/url?id=${track.id}", cover, lyrics.orEmpty())
    }

    private val shortTrack = Regex("""https://music\.163\.com/song\?id=(\d+)""")
}
