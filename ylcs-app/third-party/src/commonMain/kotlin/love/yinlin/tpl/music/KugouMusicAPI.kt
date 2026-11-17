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
@Suppress("HttpUrlsUsage")
object KugouMusicAPI : PlatformMusicAPI {
    private const val SEARCH_API = "http://mobilecdn.kugou.com/api/v3/search/song"
    private const val SONG_INFO_API = "http://m.kugou.com/app/i/getSongInfo.php"
    private const val LYRIC_SEARCH_API = "http://krcs.kugou.com/search"
    private const val LYRIC_DOWNLOAD_API = "http://lyrics.kugou.com/download"
    private val songHash = Regex("[a-fA-F0-9]{32}")

    override val type = PlatformMusicType.Kugou
    override fun matchesLink(uri: Uri): Boolean = uri.isMusicLinkOn("kugou.com")
    override fun matchesTrackId(id: String): Boolean = songHash.matches(id)

    // 保留当前曲库已有的酷狗 MOD ID，查询和导入时仍使用完整 hash。
    override fun modId(track: PlatformMusicInfo): String = type.prefix + track.id.substring(8, 24)

    override fun search(keyword: String) = flow {
        val tracks = NetClient.Common.request({
            url = "$SEARCH_API?format=json&keyword=${Uri.encodeUri(keyword)}&page=1&pagesize=20"
        }) { json: JsonObject ->
            json.obj("data").arr("info").mapNotNull { item ->
                val obj = item.Object
                val hash = obj["hash"].StringNull?.takeIf(songHash::matches) ?: return@mapNotNull null
                val filename = obj["filename"].StringNull.orEmpty()
                val name = obj["songname"].StringNull ?: filename.substringAfter(" - ").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val singer = obj["singername"].StringNull ?: filename.substringBefore(" - ", "")
                val images = obj["trans_param"].ObjectEmpty
                val pic = (images["union_cover"].StringNull ?: images["sizable_cover"].StringNull).orEmpty().replace("{size}", "512")
                PlatformMusicInfo(hash, name, singer, pic)
            }
        }
        emit(checkNotNull(tracks) { "酷狗音乐搜索失败" })
    }

    private suspend fun requestTrack(hash: String): JsonObject? = NetClient.Common.request({
        url = "$SONG_INFO_API?cmd=playInfo&hash=$hash"
    }) { json: JsonObject -> json }

    override fun findTrack(id: String) = flow {
        require(matchesTrackId(id)) { "酷狗歌曲 ID 无效" }
        val json = checkNotNull(requestTrack(id)) { "读取酷狗歌曲失败" }
        val name = checkNotNull(json["songName"].StringNull) { "未找到酷狗歌曲" }
        val singer = json["author_name"].StringNull ?: json["singerName"].StringNull.orEmpty()
        emit([PlatformMusicInfo(id, name, singer, cover(json))])
    }

    override fun parseLink(uri: Uri) = flow {
        require(matchesLink(uri)) { "请输入酷狗音乐链接" }
        val fragmentHash = uri.path?.substringAfter('#', "")?.split('&')
            ?.firstOrNull { it.startsWith("hash=", ignoreCase = true) }?.substringAfter('=')
        val hash = uri.parameterId("hash") ?: uri.parameterId("Hash") ?: fragmentHash
        require(hash != null && matchesTrackId(hash)) { "此酷狗链接缺少歌曲 hash，请使用关键词搜索" }
        findTrack(hash).collect { emit(it) }
    }

    private fun cover(json: JsonObject): String = json["imgUrl"].StringNull.orEmpty().replace("{size}", "512")

    private suspend fun requestLyrics(hash: String): String? {
        val [lyricId, accessKey] = NetClient.Common.request({
            url = "$LYRIC_SEARCH_API?keyword=%20-%20&ver=1&hash=$hash&client=mobi&man=yes"
        }) { json: JsonObject ->
            val first = json.arr("candidates").firstOrNull()?.ObjectNull
            first?.get("id").StringNull to first?.get("accesskey").StringNull
        } ?: return null

        if (lyricId.isNullOrBlank() || accessKey.isNullOrBlank()) return null
        return NetClient.Common.request({
            url = "$LYRIC_DOWNLOAD_API?charset=utf8&accesskey=${Uri.encodeUri(accessKey)}&id=${Uri.encodeUri(lyricId)}&client=mobi&fmt=lrc&ver=1"
        }) { json: JsonObject ->
            val content = json["content"].StringNull.orEmpty()
            Lrc.parse(Base64.decode(content).convert().removePrefix("\ufeff")).toString()
        }
    }

    override suspend fun resolveDownload(track: PlatformMusicInfo): PlatformMusicResources? {
        if (!matchesTrackId(track.id)) return null
        val json = requestTrack(track.id) ?: return null
        val audio = json["url"].StringNull?.takeIf(String::isNotBlank) ?: return null
        return PlatformMusicResources(audio, track.pic.ifEmpty { cover(json) }, requestLyrics(track.id).orEmpty())
    }
}
