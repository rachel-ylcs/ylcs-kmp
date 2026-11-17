package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.music.KugouMusicInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.encoding.convert
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.tpl.music.PlatformMusicRule.Kind
import love.yinlin.uri.Uri
import kotlin.io.encoding.Base64

@Stable
@Suppress("HttpUrlsUsage")
object KugouMusicManager : PlatformMusicManager(PlatformMusicType.Kugou) {
    private const val SEARCH_API = "http://mobilecdn.kugou.com/api/v3/search/song"
    private const val SONG_INFO_API = "http://m.kugou.com/app/i/getSongInfo.php"
    private const val LYRIC_SEARCH_API = "http://krcs.kugou.com/search"
    private const val LYRIC_DOWNLOAD_API = "http://lyrics.kugou.com/download"
    private val songHash = Regex("[a-fA-F0-9]{32}")

    override val rules: List<PlatformMusicRule> = [
        PlatformMusicRule.link("链接", Kind.Track, "kugou.com") { uri ->
            val fragmentHash = uri.path?.substringAfter('#', "")?.split('&')?.firstOrNull { hash ->
                hash.startsWith("hash=", ignoreCase = true)
            }?.substringAfter('=')
            (uri.parameterId("hash") ?: uri.parameterId("Hash") ?: fragmentHash)?.takeIf(songHash::matches)
        },
        PlatformMusicRule.trackId("单曲Hash", songHash),
    ]

    override fun modId(track: PlatformMusicInfo): String = type.prefix + track.id.substring(8, 24)

    override suspend fun searchKeyword(keyword: String, results: MutableList<PlatformMusicInfo>) {
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
                KugouMusicInfo(hash, name, singer, pic)
            }
        }
        results.addBatch(tracks, "搜索结果")
    }

    private suspend fun requestTrack(hash: String): JsonObject? = NetClient.Common.request({
        url = "$SONG_INFO_API?cmd=playInfo&hash=$hash"
    }) { json: JsonObject -> json }

    override suspend fun findTrack(id: String): PlatformMusicInfo? {
        val json = requestTrack(id) ?: return null
        val name = json["songName"].StringNull ?: return null
        val singer = json["author_name"].StringNull ?: json["singerName"].StringNull.orEmpty()
        return KugouMusicInfo(id, name, singer, cover(json))
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
            Base64.decode(content).convert()
        }
    }

    override suspend fun resolveDownload(track: PlatformMusicInfo): Download? {
        if (!songHash.matches(track.id)) return null
        val json = requestTrack(track.id) ?: return null
        val audio = json["url"].StringNull?.takeIf(String::isNotBlank) ?: return null
        return Download(audio, track.pic.ifEmpty { cover(json) }, requestLyrics(track.id).orEmpty())
    }
}
