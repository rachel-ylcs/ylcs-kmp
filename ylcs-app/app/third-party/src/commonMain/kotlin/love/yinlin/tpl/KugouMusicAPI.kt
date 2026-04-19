package love.yinlin.tpl

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonObject
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.tpl.lyrics.LrcParser
import love.yinlin.uri.Uri
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Stable
object KugouMusicAPI : PlatformMusicParser {

    private const val SEARCH_API = "http://mobilecdn.kugou.com/api/v3/search/song"
    private const val SONG_INFO_API = "http://m.kugou.com/app/i/getSongInfo.php"
    private const val LYRIC_SEARCH_API = "http://krcs.kugou.com/search"
    private const val LYRIC_DOWNLOAD_API = "http://lyrics.kugou.com/download"

    data class KugouSearchResult(
        val filename: String,
        val hash: String,
        val duration: Int,
        val filesize: Long,
        val albumAudioId: String = "",
        val coverUrl: String = ""
    )

    /**
     * 搜索歌曲
     */
    suspend fun searchSongs(keyword: String, page: Int = 1): List<KugouSearchResult>? {
        val url = "$SEARCH_API?format=json&keyword=${Uri.encodeUri(keyword)}&page=$page&pagesize=100"
        println("[Kugou] Search URL: $url")
        return NetClient.Common.request({
            this.url = url
        }) { json: JsonObject ->
            val infoArray = json.obj("data").arr("info")
            println("[Kugou] info array size: ${infoArray.size}")
            infoArray.mapNotNull { item ->
                try {
                    val obj = item.Object
                    val trans = obj["trans_param"]?.Object
                    val cover = trans?.get("union_cover")?.String
                        ?: trans?.get("sizable_cover")?.String
                        ?: ""
                    val albumAudioId = obj["album_audio_id"]?.String
                        ?: obj["audio_id"]?.String
                        ?: ""
                    KugouSearchResult(
                        filename = obj["filename"]?.String ?: "",
                        hash = obj["hash"]?.String ?: "",
                        duration = obj["duration"]?.Int ?: 0,
                        filesize = obj["filesize"]?.Long ?: 0L,
                        albumAudioId = albumAudioId,
                        coverUrl = cover
                    )
                } catch (e: Exception) {
                    println("[Kugou] Parse error: ${e.message}")
                    null
                }
            }
        }?.takeIf { it.isNotEmpty() }
    }

    /**
     * 获取歌曲详情（音频直链、时长、歌手等）
     */
    suspend fun getSongDetail(hash: String, coverUrl: String): PlatformMusicInfo? {
        println("[Kugou] Detail URL: $SONG_INFO_API?cmd=playInfo&hash=$hash")
        return NetClient.Common.request({
            url = "$SONG_INFO_API?cmd=playInfo&hash=$hash"
        }) { json: JsonObject ->
            val songName = json["songName"]?.String ?: ""
            val singerName = json["singerName"]?.String ?: ""
            val albumName = json["albumName"]?.String ?: ""
            val timeLength = json["timeLength"]?.Int ?: 0
            val audioUrl = json["url"]?.String ?: ""
            val finalCover = coverUrl.ifEmpty {
                json["imgUrl"]?.String?.replace("{size}", "400") ?: ""
            }.replace("{size}", "400")

            PlatformMusicInfo(
                id = hash,
                name = songName,
                singer = singerName,
                time = (timeLength * 1000L).timeString,
                pic = finalCover,
                audioUrl = audioUrl,
                lyrics = ""
            )
        }
    }

    /**
     * 公开歌词获取
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun getLyrics(hash: String): String? {
        println("[Kugou] Lyric search: hash=$hash")
        val searchResponse = NetClient.Common.request({
            url = "$LYRIC_SEARCH_API?keyword=%20-%20&ver=1&hash=$hash&client=mobi&man=yes"
        }) { json: JsonObject ->
            val candidates = json.arr("candidates")
            if (candidates.isEmpty()) {
                "" to ""
            } else {
                val first = candidates[0].Object
                first["id"]?.String to first["accesskey"]?.String
            }
        } ?: return null

        val (lyricId, accessKey) = searchResponse
        if (lyricId.isNullOrEmpty() || accessKey.isNullOrEmpty()) {
            println("[Kugou] No lyric candidate")
            return null
        }

        println("[Kugou] Lyric download: id=$lyricId")
        return NetClient.Common.request({
            url = "$LYRIC_DOWNLOAD_API?charset=utf8&accesskey=$accessKey&id=$lyricId&client=mobi&fmt=lrc&ver=1"
        }) { json: JsonObject ->
            val contentBase64 = json["content"]?.String ?: ""
            if (contentBase64.isEmpty()) return@request ""
            val lyrics = Base64.decode(contentBase64).decodeToString()
            if (lyrics.startsWith("\ufeff")) lyrics.drop(1) else lyrics
        }
    }

    suspend fun requestMusic(keyword: String): PlatformMusicInfo? {
        val searchResult = searchSongs(keyword) ?: return null
        val firstSong = searchResult.firstOrNull() ?: return null
        println("[Kugou] Selected: ${firstSong.filename}")

        val baseInfo = getSongDetail(firstSong.hash, firstSong.coverUrl) ?: return null
        // 歌词暂时单独获取，不影响基础信息返回
        val lyrics = getLyrics(firstSong.hash) ?: ""
        return baseInfo.copy(lyrics = LrcParser(lyrics).toString())
    }

    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = when {
        link.contains("kugou.com/share") -> null
        link.contains("kugou.com/songlist") -> null
        else -> Coroutines.io {
            requestMusic(link)
        }?.let { listOf(it) }
    }
}
