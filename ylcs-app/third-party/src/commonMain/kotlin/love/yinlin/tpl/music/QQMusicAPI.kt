package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.encoding.convert
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.parser.lrc.Lrc
import love.yinlin.uri.Uri
import kotlin.io.encoding.Base64

@Stable
object QQMusicAPI : PlatformMusicAPI {
    private val songMid = Regex("[A-Za-z0-9]{14}")

    override val type = PlatformMusicType.QQMusic
    override fun matchesLink(uri: Uri): Boolean = uri.isMusicLinkOn("y.qq.com")
    override fun matchesTrackId(id: String): Boolean = songMid.matches(id) && id.any(Char::isLetter) && id.any(Char::isDigit)

    private inline fun buildUrl(data: JsonObjectScope.() -> Unit): String =
        "https://u.y.qq.com/cgi-bin/musicu.fcg?data=${Uri.encodeUri(makeObject(data).toJsonString())}"

    private fun cover(track: JsonObject): String {
        val album = track["album"].ObjectEmpty
        val mid = album["pmid"].StringNull?.takeIf(String::isNotBlank) ?: album["mid"].StringNull
        return mid?.takeIf(String::isNotBlank)?.let { "https://y.qq.com/music/photo_new/T002R300x300M000${Uri.encodeUri(it)}.jpg?max_age=2592000" }.orEmpty()
    }

    private fun summary(track: JsonObject): PlatformMusicInfo? {
        val id = track["mid"].StringNull?.takeIf(songMid::matches) ?: return null
        val name = track["name"].StringNull ?: track["title"].StringNull ?: return null
        val singer = track["singer"].ArrayNull.orEmpty().mapNotNull { it.ObjectNull?.get("name").StringNull }.joinToString("、")
        return PlatformMusicInfo(id, name, singer, cover(track))
    }

    private suspend fun requestTrack(id: String): PlatformMusicInfo? = NetClient.Common.request({
        url = buildUrl {
            obj("req_0") {
                "module" with "music.pf_song_detail_svr"
                "method" with "get_song_detail_yqq"
                obj("param") { "song_mid" with id }
            }
        }
    }) { body: JsonObject ->
        summary(body.obj("req_0").obj("data").obj("track_info")) ?: error("未找到歌曲")
    }

    private suspend fun requestAlbum(id: String): List<PlatformMusicInfo>? = NetClient.Common.request({
        url = buildUrl {
            obj("req_0") {
                "module" with "music.musichallAlbum.AlbumSongList"
                "method" with "GetAlbumSongList"
                obj("param") {
                    val numericId = id.toLongOrNull()
                    if (numericId != null) "albumId" with numericId
                    else "albumMid" with id
                    "begin" with 0
                    "num" with 1000
                }
            }
        }
    }) { body: JsonObject ->
        body.obj("req_0").obj("data").arr("songList").mapNotNull { it.ObjectNull?.get("songInfo").ObjectNull?.let(::summary) }
    }

    private suspend fun requestPlaylist(id: Long): List<PlatformMusicInfo>? = NetClient.Common.request({
        url = buildUrl {
            obj("req_0") {
                "module" with "music.srfDissInfo.aiDissInfo"
                "method" with "uniform_get_Dissinfo"
                obj("param") {
                    "disstid" with id
                    "orderlist" with 1
                    "song_begin" with 0
                    "song_num" with 1000
                }
            }
        }
    }) { body: JsonObject ->
        body.obj("req_0").obj("data").arr("songlist").mapNotNull { it.ObjectNull?.let(::summary) }
    }

    override fun search(keyword: String) = flow {
        val tracks = NetClient.Common.request({
            url = buildUrl {
                obj("req_0") {
                    "module" with "music.search.SearchCgiService"
                    "method" with "DoSearchForQQMusicMobile"
                    obj("param") {
                        "query" with keyword
                        "num_per_page" with 50
                        "grp" with true
                    }
                }
            }
        }) { body: JsonObject ->
            body.obj("req_0").obj("data").obj("body").arr("item_song").mapNotNull { it.ObjectNull?.let(::summary) }
        }
        emit(checkNotNull(tracks) { "QQ音乐搜索失败" })
    }

    override fun findTrack(id: String) = flow {
        require(songMid.matches(id)) { "QQ音乐歌曲 ID 无效" }
        emit([checkNotNull(requestTrack(id)) { "未找到 QQ音乐歌曲" }])
    }

    override fun parseLink(uri: Uri) = flow {
        require(matchesLink(uri)) { "请输入 QQ音乐链接" }
        val target = if (uri.isMusicLinkOn("c6.y.qq.com")) {
            val redirected = NetClient.Common.request<String, String>({ url = uri.toString() }) { url }
            redirected?.let(Uri::parse)?.takeIf { it.isMusicLinkOn("y.qq.com") } ?: error("无法解析 QQ音乐短链接")
        } else uri
        val path = target.path.orEmpty()
        val id = target.parameterId("id") ?: target.lastPathSegment().orEmpty()
        when {
            path.contains("songDetail") -> findTrack(target.parameterId("songmid") ?: id).collect { emit(it) }
            path.contains("playlist") || path.contains("taoge") -> {
                val playlistId = (target.parameterId("disstid") ?: id).toLongOrNull() ?: error("链接缺少有效歌单 ID")
                emit(checkNotNull(requestPlaylist(playlistId)) { "读取 QQ音乐歌单失败" })
            }
            path.contains("albumDetail") -> {
                require(id.toLongOrNull() != null || songMid.matches(id)) { "链接缺少有效专辑 ID" }
                emit(checkNotNull(requestAlbum(id)) { "读取 QQ音乐专辑失败" })
            }
            else -> error("此 QQ音乐链接不包含可解析的单曲、歌单或专辑")
        }
    }

    override suspend fun resolveDownload(track: PlatformMusicInfo): PlatformMusicResources? {
        val id = track.id
        if (!songMid.matches(id)) return null
        val coverUrl = track.pic.ifEmpty { requestTrack(id)?.pic.orEmpty() }
        return NetClient.Common.request({
            url = buildUrl {
                obj("req_0") {
                    "module" with "music.musichallSong.PlayLyricInfo"
                    "method" with "GetPlayLyricInfo"
                    obj("param") { "songMID" with id }
                }
                obj("req_1") {
                    "module" with "vkey.GetVkeyServer"
                    "method" with "CgiGetVkey"
                    obj("param") {
                        arr("filename") { add("C400$id$id.m4a") }
                        arr("songmid") { add(id) }
                        arr("songtype") { add(0) }
                        "guid" with "19911211"
                    }
                }
            }
        }) { body: JsonObject ->
            val file = body.obj("req_1").obj("data").arr("midurlinfo").firstOrNull()?.ObjectNull
                ?.get("purl").StringNull?.takeIf(String::isNotBlank) ?: error("QQ音乐未返回可下载的音频")
            val lyrics = body["req_0"].ObjectNull?.get("data").ObjectNull?.get("lyric").StringNull.orEmpty()
            val lrc = catchingNull { Lrc.parse(Base64.decode(lyrics).convert()).toString() }.orEmpty()
            PlatformMusicResources("https://ws.stream.qqmusic.qq.com/$file", coverUrl, lrc)
        }
    }
}
