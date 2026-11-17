package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.music.QQMusicInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.encoding.convert
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.tpl.music.PlatformMusicRule.Kind
import love.yinlin.uri.Uri
import kotlin.io.encoding.Base64

@Stable
object QQMusicManager : PlatformMusicManager(PlatformMusicType.QQMusic) {
    private val songMid = Regex("[A-Za-z0-9]{14}")

    override val rules: List<PlatformMusicRule> = [
        // 解析短链
        PlatformMusicRule.redirect("短链接", "c6.y.qq.com"),
        // 歌曲 https://y.qq.com/n/ryqq/songDetail/003yJ3Ba1bDVJc
        PlatformMusicRule.link("单曲", Kind.Track, "y.qq.com") { uri ->
            (uri.parameterId("songmid") ?: uri.parameterId("id") ?: uri.lastPathSegment())?.takeIf { id ->
                uri.path.orEmpty().contains("songDetail") && songMid.matches(id)
            }
        },
        // 歌单 https://i2.y.qq.com/n3/other/pages/share/personalized_playlist_v2/index.html?id=9094549201
        // 歌单 https://i.y.qq.com/n2/m/share/details/taoge.html?id=9094549201
        // 歌单 https://y.qq.com/n/ryqq/playlist/9094549201
        PlatformMusicRule.link("歌单", Kind.Playlist, "y.qq.com") { uri ->
            val path = uri.path.orEmpty()
            (uri.parameterId("disstid") ?: uri.parameterId("id") ?: uri.lastPathSegment())?.takeIf { id ->
                (path.contains("playlist") || path.contains("taoge")) && id.toLongOrNull() != null
            }
        },
        // 解析专辑
        PlatformMusicRule.link("专辑", Kind.Album, "y.qq.com") { uri ->
            (uri.parameterId("id") ?: uri.lastPathSegment())?.takeIf { id ->
                uri.path.orEmpty().contains("albumDetail") && (id.toLongOrNull() != null || songMid.matches(id))
            }
        },
        // 歌曲 003yJ3Ba1bDVJc
        PlatformMusicRule.trackId("单曲MID", Regex("(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9]{14}")),
    ]

    private inline fun buildUrl(data: JsonObjectScope.() -> Unit): String = "https://u.y.qq.com/cgi-bin/musicu.fcg?data=${Uri.encodeUri(makeObject(data).toJsonString())}"

    private fun cover(track: JsonObject): String {
        val album = track["album"].ObjectEmpty
        val mid = album["pmid"].StringNull?.takeIf(String::isNotBlank) ?: album["mid"].StringNull
        return mid?.takeIf(String::isNotBlank)?.let { "https://y.qq.com/music/photo_new/T002R500x500M000${Uri.encodeUri(it)}.jpg?max_age=2592000" }.orEmpty()
    }

    private fun summary(track: JsonObject): PlatformMusicInfo? {
        val id = track["mid"].StringNull?.takeIf(songMid::matches) ?: return null
        val name = track["name"].StringNull ?: track["title"].StringNull ?: return null
        val singer = track["singer"].ArrayNull.orEmpty().mapNotNull { it.ObjectNull?.get("name").StringNull }.joinToString("、")
        return QQMusicInfo(id, name, singer, cover(track))
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

    override suspend fun searchKeyword(keyword: String, results: MutableList<PlatformMusicInfo>) {
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
        results.addBatch(tracks, "搜索结果")
    }

    override suspend fun findTrack(id: String): PlatformMusicInfo? = requestTrack(id)

    override suspend fun findPlaylist(id: String, results: MutableList<PlatformMusicInfo>) {
        results.addBatch(requestPlaylist(id.toLong()), "歌单")
    }

    override suspend fun findAlbum(id: String, results: MutableList<PlatformMusicInfo>) {
        results.addBatch(requestAlbum(id), "专辑")
    }

    override suspend fun resolveDownload(track: PlatformMusicInfo): Download? {
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
            val lrc = catchingNull { Base64.decode(lyrics).convert() }.orEmpty()
            Download("https://ws.stream.qqmusic.qq.com/$file", coverUrl, lrc)
        }
    }
}
