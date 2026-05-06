package love.yinlin.tpl

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonObject
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.tpl.lyrics.LrcParser
import love.yinlin.uri.Uri

@Stable
object MiguMusicAPI : PlatformMusicAPI {

    private const val SEARCH_API = "https://c.musicapp.migu.cn/v1.0/content/search_all.do"
    private const val DETAIL_API = "https://c.musicapp.migu.cn/MIGUM3.0/strategy/listen-url/v2.4"
    private const val ALBUM_SONGLIST_API = "https://app.c.nf.migu.cn/MIGUM3.0/resource/album/song/v2.0"
    private const val PLAYLIST_SONGLIST_API = "https://app.c.nf.migu.cn/MIGUM3.0/resource/playlist/song/v2.0"

    private val defaultHeaders = io.ktor.http.headers {
        append("channel", "0146921")
        append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
    }

    data class MiguSearchResult(
        val contentId: String,
        val copyrightId: String,
        val name: String,
        val singers: String,
        val imgUrl: String,
        val lyricUrl: String?,
        val duration: Int,
        val album: String
    )

    /**
     * 搜索歌曲（仅获取摘要信息）
     */
    suspend fun searchSongs(keyword: String): List<MiguSearchResult>? {
        val searchSwitch = """{"song":1,"album":0,"singer":0,"tagSong":1,"mvSong":0,"bestShow":1}"""
        val url = "$SEARCH_API?text=${Uri.encodeUri(keyword)}&searchSwitch=${Uri.encodeUri(searchSwitch)}&pageSize=50&pageNo=1"

        return NetClient.Common.request({
            this.url = url
            headers = defaultHeaders
        }) { json: JsonObject ->
            json.obj("songResultData").arr("result").mapNotNull { item ->
                val obj = item.Object
                val singers = obj.arr("singers").joinToString(",") { it.Object["name"].String }
                val imgItems = obj.arr("imgItems")
                val imgUrl = if (imgItems.isNotEmpty()) imgItems.last().Object["img"].String else ""
                MiguSearchResult(
                    contentId = obj["contentId"].String,
                    copyrightId = obj["copyrightId"].String,
                    name = obj["name"].String,
                    singers = singers,
                    imgUrl = imgUrl,
                    lyricUrl = obj["lyricUrl"]?.String,
                    duration = obj["duration"]?.Int ?: 0,
                    album = obj["album"]?.String ?: ""
                )
            }
        }?.ifEmpty { null }
    }

    /**
     * 获取歌曲详情（音频直链、歌词等）
     */
    suspend fun requestMusic(result: MiguSearchResult): PlatformMusicInfo? {
        val detailUrl = "$DETAIL_API?resourceType=2&toneFlag=HQ&contentId=${result.contentId}&copyrightId=${result.copyrightId}&lowerQualityContentId=${result.contentId}"

        val detailJson: JsonObject = NetClient.Common.request<JsonObject>({
            url = detailUrl
            headers = defaultHeaders
        }) { json: JsonObject -> json } ?: return null

        if (detailJson["code"]?.String != "000000") return null

        val data = detailJson.obj("data")
        val audioUrl = data["url"].String
        val lrcUrl = data["lrcUrl"]?.String ?: result.lyricUrl ?: ""
        val lyrics = if (lrcUrl.isNotEmpty()) {
            NetClient.Common.request({
                url = lrcUrl
                headers = defaultHeaders
            }) { text: String -> LrcParser(text).toString() } ?: ""
        } else ""

        var pic = result.imgUrl
        if (pic.startsWith("/")) pic = "https://d.musicapp.migu.cn$pic"

        return PlatformMusicInfo(
            id = result.contentId,
            name = result.name,
            singer = result.singers,
            time = (result.duration * 1000L).timeString,
            pic = pic,
            audioUrl = audioUrl,
            lyrics = lyrics
        )
    }

    /**
     * 获取专辑歌曲列表
     */
    private suspend fun requestAlbumSongs(albumId: String): List<MiguSearchResult>? =
        NetClient.Common.request({
            url = "$ALBUM_SONGLIST_API?pageNo=1&pageSize=200&albumId=$albumId"
            headers = defaultHeaders
        }) { json: JsonObject ->
            json.obj("data").arr("songList").mapNotNull { item ->
                val obj = item.Object
                val singers = obj.arr("singerList").joinToString(",") { it.Object["name"].String }
                val rawImg = obj["img3"]?.String ?: obj["img2"]?.String ?: obj["img1"]?.String ?: ""
                val imgUrl = if (rawImg.startsWith("/")) "https://d.musicapp.migu.cn$rawImg" else rawImg
                MiguSearchResult(
                    contentId = obj["contentId"].String,
                    copyrightId = obj["copyrightId"].String,
                    name = obj["songName"].String,
                    singers = singers,
                    imgUrl = imgUrl,
                    lyricUrl = null,
                    duration = obj["duration"]?.Int ?: 0,
                    album = obj["album"]?.String ?: ""
                )
            }
        }?.ifEmpty { null }

    /**
     * 获取歌单歌曲列表
     */
    private suspend fun requestPlaylistSongs(playlistId: String): List<MiguSearchResult>? =
        NetClient.Common.request({
            url = "$PLAYLIST_SONGLIST_API?pageNo=1&pageSize=200&playlistId=$playlistId"
            headers = defaultHeaders
        }) { json: JsonObject ->
            json.obj("data").arr("songList").mapNotNull { item ->
                val obj = item.Object
                val singers = obj.arr("singerList").joinToString(",") { it.Object["name"].String }
                val rawImg = obj["img3"]?.String ?: obj["img2"]?.String ?: obj["img1"]?.String ?: ""
                val imgUrl = if (rawImg.startsWith("/")) "https://d.musicapp.migu.cn$rawImg" else rawImg
                MiguSearchResult(
                    contentId = obj["contentId"].String,
                    copyrightId = obj["copyrightId"].String,
                    name = obj["songName"].String,
                    singers = singers,
                    imgUrl = imgUrl,
                    lyricUrl = null,
                    duration = obj["duration"]?.Int ?: 0,
                    album = obj["album"]?.String ?: ""
                )
            }
        }?.ifEmpty { null }

    /**
     * 根据单曲ID获取摘要（用于单曲短链接）
     */
    private suspend fun requestSongById(songId: String): MiguSearchResult? {
        val detailUrl = "$DETAIL_API?resourceType=2&toneFlag=HQ&contentId=$songId&lowerQualityContentId=$songId"
        val json = NetClient.Common.request<JsonObject>({
            url = detailUrl
            headers = defaultHeaders
        }) { json: JsonObject -> json } ?: return null

        val songObj = json.obj("data").obj("song")
        val singers = songObj.arr("singerList").joinToString(",") { it.Object["name"].String }
        val imgUrl = songObj["img3"]?.String ?: songObj["img2"]?.String ?: songObj["img1"]?.String ?: ""

        return MiguSearchResult(
            contentId = songObj["contentId"]?.String ?: songId,
            copyrightId = songObj["copyrightId"]?.String ?: "",
            name = songObj["songName"]?.String ?: "",
            singers = singers,
            imgUrl = imgUrl,
            lyricUrl = null,
            duration = songObj["duration"]?.Int ?: 0,
            album = songObj["album"]?.String ?: ""
        )
    }

    /**
     * 解析短链接（重定向后从最终 URL 提取类型和 ID）
     */
    private suspend fun resolveLink(link: String): Pair<String, String>? {
        val finalUrl: String = NetClient.Common.request<String, String>({
            url = link
            headers = defaultHeaders
        }) {
            url
        } ?: return null

        val params = Uri.parse(finalUrl)?.params ?: return null
        val id = params["id"] ?: return null

        return when {
            finalUrl.contains("album") -> "album" to id
            finalUrl.contains("playlist") -> "playlist" to id
            finalUrl.contains("song") -> "song" to id
            else -> null
        }
    }

    override suspend fun search(keyword: String): List<PlatformMusicInfo>? {
        val result = searchSongs(keyword) ?: return null
        return result.mapNotNull { requestMusic(it) }.ifEmpty { null }
    }

    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = Coroutines.io {
        when {
            link.contains("c.migu.cn") -> {
                resolveLink(link)?.let { (type, id) ->
                    val songs = when (type) {
                        "album" -> requestAlbumSongs(id)
                        "playlist" -> requestPlaylistSongs(id)
                        "song" -> requestSongById(id)?.let(::listOf)
                        else -> null
                    } ?: return@let null

                    songs.mapNotNull { requestMusic(it) }.ifEmpty { null }
                }
            }
            else -> search(link)
        }
    }
}