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
object KugouMusicAPI : PlatformMusicAPI {

    private const val SEARCH_API = "http://mobilecdn.kugou.com/api/v3/search/song"
    private const val SONG_INFO_API = "http://m.kugou.com/app/i/getSongInfo.php"
    private const val LYRIC_SEARCH_API = "http://krcs.kugou.com/search"
    private const val LYRIC_DOWNLOAD_API = "http://lyrics.kugou.com/download"

    data class KugouSearchResult(
        val filename: String,
        val hash: String,
        val duration: Int,
        val filesize: Long,
        val coverUrl: String = ""
    )

    /**
     * 从完整 hash 中提取中间 16 位作为 ID
     */
    private fun extractShortId(hash: String): String {
        return if (hash.length >= 24) {
            hash.substring(8, 24)
        } else if (hash.length >= 16) {
            val start = (hash.length - 16) / 2
            hash.substring(start, start + 16)
        } else {
            hash
        }
    }

    /**
     * 搜索歌曲（仅获取摘要信息）
     */
    suspend fun searchSongs(keyword: String, page: Int = 1): List<KugouSearchResult>? {
        val url = "$SEARCH_API?format=json&keyword=${Uri.encodeUri(keyword)}&page=$page&pagesize=100"
        return NetClient.Common.request({
            this.url = url
        }) { json: JsonObject ->
            json.obj("data").arr("info").mapNotNull { item ->
                val obj = item.Object
                val trans = obj["trans_param"]?.Object
                val cover = trans?.get("union_cover")?.String
                    ?: trans?.get("sizable_cover")?.String
                    ?: ""
                KugouSearchResult(
                    filename = obj["filename"]?.String ?: "",
                    hash = obj["hash"]?.String ?: "",
                    duration = obj["duration"]?.Int ?: 0,
                    filesize = obj["filesize"]?.Long ?: 0L,
                    coverUrl = cover
                )
            }
        }?.takeIf { it.isNotEmpty() }
    }

    /**
     * 获取歌曲详情（音频直链、时长、多歌手等）
     */
    suspend fun getSongDetail(hash: String, coverUrl: String): PlatformMusicInfo? {
        return NetClient.Common.request({
            url = "$SONG_INFO_API?cmd=playInfo&hash=$hash"
        }) { json: JsonObject ->
            val songName = json["songName"]?.String ?: ""
            // 关键修正：从 author_name 获取完整的多歌手字符串
            val singerName = json["author_name"]?.String
                ?: json["singerName"]?.String
                ?: ""
            val timeLength = json["timeLength"]?.Int ?: 0
            val audioUrl = json["url"]?.String ?: ""
            val finalCover = coverUrl.ifEmpty {
                json["imgUrl"]?.String?.replace("{size}", "400") ?: ""
            }.replace("{size}", "400")

            PlatformMusicInfo(
                id = extractShortId(hash),
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
        if (lyricId.isNullOrEmpty() || accessKey.isNullOrEmpty()) return null

        return NetClient.Common.request({
            url = "$LYRIC_DOWNLOAD_API?charset=utf8&accesskey=$accessKey&id=$lyricId&client=mobi&fmt=lrc&ver=1"
        }) { json: JsonObject ->
            val contentBase64 = json["content"]?.String ?: ""
            if (contentBase64.isEmpty()) return@request ""
            val lyrics = Base64.decode(contentBase64).decodeToString()
            if (lyrics.startsWith("\ufeff")) lyrics.drop(1) else lyrics
        }
    }

    /**
     * 完整获取单首歌曲信息（包含歌词）
     */
    suspend fun requestMusic(hash: String, coverUrl: String): PlatformMusicInfo? {
        val baseInfo = getSongDetail(hash, coverUrl) ?: return null
        val lyrics = getLyrics(hash) ?: ""
        return baseInfo.copy(lyrics = LrcParser(lyrics).toString())
    }

    // ========== 实现 PlatformMusicAPI 接口 ==========

    override suspend fun search(keyword: String): List<PlatformMusicInfo>? {
        val searchResult = searchSongs(keyword) ?: return null
        return searchResult.mapNotNull { song ->
            requestMusic(song.hash, song.coverUrl)
        }.ifEmpty { null }
    }

    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = null
}
