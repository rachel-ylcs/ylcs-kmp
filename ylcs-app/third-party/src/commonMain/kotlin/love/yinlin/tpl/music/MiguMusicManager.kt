package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.music.MiguMusicInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.http.NetHeader
import love.yinlin.tpl.music.PlatformMusicRule.Kind
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri

@Stable
object MiguMusicManager : PlatformMusicManager(PlatformMusicType.Migu) {
    private const val SEARCH_API = "https://c.musicapp.migu.cn/v1.0/content/search_all.do"
    private const val DETAIL_API = "https://c.musicapp.migu.cn/MIGUM2.0/v1.0/content/resourceinfo.do"
    private const val LISTEN_API = "https://c.musicapp.migu.cn/MIGUM2.0/v2.1/content/listen-url"
    private const val ALBUM_SONGLIST_API = "https://app.c.nf.migu.cn/MIGUM3.0/resource/album/song/v2.0"
    private const val PLAYLIST_SONGLIST_API = "https://app.c.nf.migu.cn/MIGUM3.0/resource/playlist/song/v2.0"
    private val resourceId = Regex("[A-Za-z0-9]{1,64}")

    private val defaultHeaders: NetHeader = [
        "channel" to "0146921",
        HttpHeaders.UserAgent to NetHeader.UserAgentDesktop,
        HttpHeaders.Referrer to "https://music.migu.cn/",
    ]

    override val rules: List<PlatformMusicRule> = [
        PlatformMusicRule.redirect("短链接", "c.migu.cn"),
        musicLink("专辑", Kind.Album, "album", "albumId"),
        musicLink("歌单", Kind.Playlist, "playlist", "playlistId"),
        musicLink("单曲", Kind.Track, "song", "contentId", "copyrightId", "songId"),
    ]

    private fun musicLink(description: String, kind: Kind, path: String, vararg parameters: String): PlatformMusicRule =
        PlatformMusicRule.link(description, kind, "music.migu.cn", "musicapp.migu.cn", "h5.nf.migu.cn") { uri ->
            val id = uri.parameterId("id") ?: parameters.firstNotNullOfOrNull { uri.parameterId(it) } ?: uri.lastPathSegment()
            id?.takeIf { uri.path.orEmpty().contains(path) && resourceId.matches(it) }
    }

    override val redirectHeaders: NetHeader get() = defaultHeaders

    private fun summary(song: JsonObject): MiguMusicInfo? {
        val id = song["contentId"].StringNull?.takeIf(resourceId::matches) ?: return null
        val name = song["songName"].StringNull ?: song["name"].StringNull ?: return null
        val singers = song["singerList"].ArrayNull ?: song["singers"].ArrayNull.orEmpty()
        val singer = singers.mapNotNull { it.ObjectNull?.get("name").StringNull }.joinToString("、").ifEmpty { song["singer"].StringNull.orEmpty() }
        val images = song["imgItems"].ArrayNull ?: song["albumImgs"].ArrayNull
        val pic = images?.lastOrNull()?.ObjectNull?.get("img").StringNull ?: song["img3"].StringNull ?: song["img2"].StringNull ?: song["img1"].StringNull.orEmpty()
        return MiguMusicInfo(
            id = id,
            name = name,
            singer = singer,
            pic = if (pic.startsWith("/")) "https://d.musicapp.migu.cn$pic" else pic,
            copyrightId = song["copyrightId"].StringNull.orEmpty(),
            lyricUrl = song["lrcUrl"].StringNull?.takeIf(String::isNotBlank) ?: song["lyricUrl"].StringNull,
        )
    }

    override suspend fun searchKeyword(keyword: String, results: MutableList<PlatformMusicInfo>) {
        val searchSwitch = """{"song":1,"album":0,"singer":0,"tagSong":1,"mvSong":0,"bestShow":1}"""
        val tracks = NetClient.Common.request({
            url = "$SEARCH_API?text=${Uri.encodeUri(keyword)}&searchSwitch=${Uri.encodeUri(searchSwitch)}&pageSize=50&pageNo=1"
            headers = defaultHeaders
        }) { json: JsonObject ->
            json.obj("songResultData").arr("result").mapNotNull { it.ObjectNull?.let(::summary) }
        }
        results.addBatch(tracks, "搜索结果")
    }

    private suspend fun requestSongList(id: String, isAlbum: Boolean): List<PlatformMusicInfo>? = NetClient.Common.request({
        url = if (isAlbum) "$ALBUM_SONGLIST_API?pageNo=1&pageSize=200&albumId=$id" else "$PLAYLIST_SONGLIST_API?pageNo=1&pageSize=200&playlistId=$id"
        headers = defaultHeaders
    }) { json: JsonObject ->
        json.obj("data").arr("songList").mapNotNull { it.ObjectNull?.let(::summary) }
    }

    private suspend fun requestDetail(id: String, parameter: String): JsonObject? = NetClient.Common.request({
        url = "$DETAIL_API?resourceType=2&$parameter=$id"
        headers = defaultHeaders
    }) { json: JsonObject ->
        check(json["code"].StringNull == "000000")
        json.arr("resource").firstNotNullOfOrNull { item ->
            val song = item.ObjectNull ?: return@firstNotNullOfOrNull null
            song.takeIf { id == it["contentId"].StringNull || id == it["songId"].StringNull || id == it["copyrightId"].StringNull }
        } ?: error("未找到歌曲")
    }

    override suspend fun findTrack(id: String): PlatformMusicInfo? =
        (requestDetail(id, "resourceId") ?: requestDetail(id, "copyrightId"))?.let(::summary)

    override suspend fun findPlaylist(id: String, results: MutableList<PlatformMusicInfo>) {
        results.addBatch(requestSongList(id, false), "歌单")
    }

    override suspend fun findAlbum(id: String, results: MutableList<PlatformMusicInfo>) {
        results.addBatch(requestSongList(id, true), "专辑")
    }

    override suspend fun resolveDownload(track: PlatformMusicInfo): Download? {
        if (track !is MiguMusicInfo || !resourceId.matches(track.id)) return null

        val data = NetClient.Common.request({
            val copyright = track.copyrightId.takeIf(String::isNotBlank)?.let { "&copyrightId=${Uri.encodeUri(it)}" }.orEmpty()
            url = "$LISTEN_API?netType=01&resourceType=2&contentId=${track.id}&toneFlag=PQ$copyright"
            headers = defaultHeaders
        }) { json: JsonObject ->
            check(json["code"].StringNull == "000000")
            json.obj("data")
        } ?: return null

        if (data["freeListenType"].StringNull != "0" || data["formatType"].StringNull != "PQ") return null
        val song = data.obj("songItem")
        if (song["contentId"].StringNull != track.id || song["needEncrypt"].StringNull == "1") return null
        val vip = song["vipType"].StringNull.orEmpty()
        if ((vip.isNotEmpty() && vip != "0") || song["chargeAuditions"].StringNull == "1") return null
        val audio = data["url"].StringNull?.let(Uri::parse) ?: return null
        if (audio.scheme != Scheme.Https || audio.host != "freetyst.nf.migu.cn" || !audio.path.orEmpty().endsWith(".mp3", ignoreCase = true)) return null
        val detail = summary(song)
        val lyricUrl = detail?.lyricUrl ?: track.lyricUrl
        val lyrics = lyricUrl?.let { url ->
            NetClient.Common.request({
                this.url = url
                headers = defaultHeaders
            }) { text: String -> text }
        }
        val cover = detail?.pic.orEmpty().ifEmpty { track.pic }

        return Download(audio.toString(), cover, lyrics.orEmpty())
    }
}
