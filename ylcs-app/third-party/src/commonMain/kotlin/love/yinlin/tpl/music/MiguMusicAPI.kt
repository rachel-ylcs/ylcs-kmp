package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.http.NetHeader
import love.yinlin.foundation.parser.lrc.Lrc
import love.yinlin.uri.Uri

@Stable
object MiguMusicAPI : PlatformMusicAPI {
    private const val SEARCH_API = "https://c.musicapp.migu.cn/v1.0/content/search_all.do"
    private const val DETAIL_API = "https://c.musicapp.migu.cn/MIGUM3.0/strategy/listen-url/v2.4"
    private const val ALBUM_SONGLIST_API = "https://app.c.nf.migu.cn/MIGUM3.0/resource/album/song/v2.0"
    private const val PLAYLIST_SONGLIST_API = "https://app.c.nf.migu.cn/MIGUM3.0/resource/playlist/song/v2.0"
    private val resourceId = Regex("[A-Za-z0-9]{1,64}")

    private val defaultHeaders: NetHeader = [
        "channel" to "0146921",
        HttpHeaders.UserAgent to NetHeader.UserAgentDesktop
    ]

    override val type = PlatformMusicType.Migu
    override fun matchesLink(uri: Uri): Boolean = uri.isMusicLinkOn("c.migu.cn") || uri.isMusicLinkOn("music.migu.cn") || uri.isMusicLinkOn("musicapp.migu.cn")

    private fun summary(song: JsonObject): PlatformMusicInfo? {
        val id = song["contentId"].StringNull?.takeIf(resourceId::matches) ?: return null
        val name = song["songName"].StringNull ?: song["name"].StringNull ?: return null
        val singers = song["singerList"].ArrayNull ?: song["singers"].ArrayNull.orEmpty()
        val singer = singers.mapNotNull { it.ObjectNull?.get("name").StringNull }.joinToString("、")
        val pic = song["imgItems"].ArrayNull?.lastOrNull()?.ObjectNull?.get("img").StringNull
            ?: song["img3"].StringNull ?: song["img2"].StringNull ?: song["img1"].StringNull.orEmpty()
        return PlatformMusicInfo(
            id = id,
            name = name,
            singer = singer,
            pic = if (pic.startsWith("/")) "https://d.musicapp.migu.cn$pic" else pic,
            miguSource = PlatformMusicInfo.MiguSource(song["copyrightId"].StringNull.orEmpty(), song["lyricUrl"].StringNull)
        )
    }

    override fun search(keyword: String) = flow {
        val searchSwitch = """{"song":1,"album":0,"singer":0,"tagSong":1,"mvSong":0,"bestShow":1}"""
        val tracks = NetClient.Common.request({
            url = "$SEARCH_API?text=${Uri.encodeUri(keyword)}&searchSwitch=${Uri.encodeUri(searchSwitch)}&pageSize=50&pageNo=1"
            headers = defaultHeaders
        }) { json: JsonObject ->
            json.obj("songResultData").arr("result").mapNotNull { it.ObjectNull?.let(::summary) }
        }
        emit(checkNotNull(tracks) { "咪咕音乐搜索失败" })
    }

    private suspend fun requestSongList(id: String, isAlbum: Boolean): List<PlatformMusicInfo>? = NetClient.Common.request({
        url = if (isAlbum) "$ALBUM_SONGLIST_API?pageNo=1&pageSize=200&albumId=$id"
            else "$PLAYLIST_SONGLIST_API?pageNo=1&pageSize=200&playlistId=$id"
        headers = defaultHeaders
    }) { json: JsonObject ->
        json.obj("data").arr("songList").mapNotNull { it.ObjectNull?.let(::summary) }
    }

    private suspend fun requestDetail(id: String, copyrightId: String = ""): JsonObject? = NetClient.Common.request({
        val copyright = if (copyrightId.isEmpty()) "" else "&copyrightId=${Uri.encodeUri(copyrightId)}"
        url = "$DETAIL_API?resourceType=2&toneFlag=HQ&contentId=$id&lowerQualityContentId=$id$copyright"
        headers = defaultHeaders
    }) { json: JsonObject ->
        check(json["code"].StringNull == "000000")
        json.obj("data")
    }

    override fun findTrack(id: String) = flow {
        require(resourceId.matches(id)) { "咪咕歌曲 ID 无效" }
        val song = requestDetail(id)?.get("song").ObjectNull?.let(::summary)
        emit([checkNotNull(song) { "未找到咪咕歌曲" }])
    }

    override fun parseLink(uri: Uri) = flow {
        require(matchesLink(uri)) { "请输入咪咕音乐链接" }
        val target = if (uri.isMusicLinkOn("c.migu.cn")) {
            val redirected = NetClient.Common.request<String, String>({
                url = uri.toString()
                headers = defaultHeaders
            }) { url }
            redirected?.let(Uri::parse)?.takeIf(::matchesLink) ?: error("无法解析咪咕音乐短链接")
        } else uri
        val path = target.path.orEmpty()
        val id = (target.parameterId("id") ?: target.lastPathSegment())?.takeIf(resourceId::matches) ?: error("链接缺少有效 ID")
        when {
            path.contains("album") -> emit(checkNotNull(requestSongList(id, true)) { "读取咪咕专辑失败" })
            path.contains("playlist") -> emit(checkNotNull(requestSongList(id, false)) { "读取咪咕歌单失败" })
            path.contains("song") -> findTrack(id).collect { emit(it) }
            else -> error("此咪咕链接不包含可解析的单曲、歌单或专辑")
        }
    }

    override suspend fun resolveDownload(track: PlatformMusicInfo): PlatformMusicResources? {
        if (!resourceId.matches(track.id)) return null
        val data = requestDetail(track.id, track.miguSource?.copyrightId.orEmpty()) ?: return null
        val audioUrl = data["url"].StringNull?.takeIf(String::isNotBlank) ?: return null
        val lyricUrl = data["lrcUrl"].StringNull?.takeIf(String::isNotBlank) ?: track.miguSource?.lyricUrl
        val lyrics = lyricUrl?.let { url ->
            NetClient.Common.request({
                this.url = url
                headers = defaultHeaders
            }) { text: String -> Lrc.parse(text).toString() }
        }
        val cover = track.pic.ifEmpty { data["song"].ObjectNull?.let(::summary)?.pic.orEmpty() }
        return PlatformMusicResources(audioUrl, cover, lyrics.orEmpty())
    }
}
