package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.http.NetHeader
import love.yinlin.foundation.parser.lrc.Lrc
import love.yinlin.uri.Uri

@Stable
object SodaMusicAPI : PlatformMusicAPI {
    private const val SEARCH_API = "https://api.qishui.com/luna/pc/search/track"
    private const val PLAYLIST_SHARE_PAGE = "https://music.douyin.com/qishui/share/playlist"
    private const val TRACK_PUBLIC_API = "https://beta-luna.douyin.com/luna/h5/seo_track"
    private const val TRACK_SHARE_PAGE = "https://music.douyin.com/qishui/share/track"

    private val defaultHeaders: NetHeader = [
        HttpHeaders.UserAgent to NetHeader.UserAgentDesktop,
        HttpHeaders.Accept to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        HttpHeaders.AcceptLanguage to NetHeader.AcceptLanguageDefault,
        HttpHeaders.Origin to "https://music.douyin.com",
        HttpHeaders.Referrer to "https://music.douyin.com/",
    ]

    private val routerData = Regex("""(?s)_ROUTER_DATA\s*=\s*(\{.*?\});""")
    private val lyricLine = Regex("""\[(\d+),(\d+)](.*)""")
    private val lyricTag = Regex("""<[^>]+>""")

    override val type = PlatformMusicType.Soda
    override fun matchesTrackId(id: String): Boolean = id.length == 19 && numericTrackId.matches(id)
    override fun matchesLink(uri: Uri): Boolean = uri.isMusicLinkOn("qishui.com") ||
        uri.isMusicLinkOn("qishui.douyin.com") || uri.isMusicLinkOn("music.douyin.com")

    private fun summary(track: JsonObject, id: String): PlatformMusicInfo? {
        val name = track["name"].StringNull ?: return null
        val singer = track["artists"].ArrayNull.orEmpty().mapNotNull { it.ObjectNull?.get("name").StringNull }.joinToString("、")
        val cover = track["album"].ObjectNull?.get("url_cover").ObjectEmpty
        val url = cover["urls"].ArrayNull?.firstOrNull().StringNull
        val uri = cover["uri"].StringNull
        val pic = if (url != null && uri != null) "$url$uri~c5_375x375.jpg" else ""
        return PlatformMusicInfo(id, name, singer, pic)
    }

    private suspend fun requestTrack(id: String): JsonObject? = NetClient.Common.request({
        url = "$TRACK_PUBLIC_API?track_id=$id&device_platform=web"
        headers = defaultHeaders
    }) { json: JsonObject -> json }

    private suspend fun requestSharePage(address: String, page: String): JsonObject? = NetClient.Common.request({
        url = address
        headers = defaultHeaders
    }) { html: String ->
        val data = routerData.find(html)?.groupValues?.get(1) ?: error("分享页缺少歌曲信息")
        Json.decodeFromString<JsonObject>(data).obj("loaderData").obj(page)
    }

    private fun publicTrack(data: JsonObject?): JsonObject? = data?.get("seo_track").ObjectNull?.get("track").ObjectNull

    // 有摘要的条目先显示，缺失摘要的条目再逐首补查。
    private suspend fun FlowCollector<List<PlatformMusicInfo>>.emitTracks(rows: List<JsonObject>) {
        val ready: MutableList<PlatformMusicInfo> = []
        val pending: MutableSet<String> = mutableSetOf()
        for (row in rows) {
            val entity = row["entity"].ObjectNull ?: row
            val track = entity["track"].ObjectNull ?: entity["track_wrapper"].ObjectNull?.get("track").ObjectNull ?: entity
            val id = (track["id"].StringNull ?: row["id"].StringNull)?.takeIf(numericTrackId::matches) ?: continue
            val info = summary(track, id)
            if (info != null) ready += info
            else pending += id
        }
        if (ready.isNotEmpty()) {
            pending.removeAll(ready.map { it.id }.toSet())
            emit(ready)
        }

        var failures = 0
        for (id in pending) {
            val info = publicTrack(requestTrack(id))?.let { summary(it, id) }
            if (info != null) emit([info])
            else failures++
        }
        check(failures == 0) { if (ready.isEmpty() && failures == pending.size) "读取汽水音乐歌曲失败" else "部分汽水音乐歌曲读取失败" }
    }

    override fun search(keyword: String) = flow {
        val tracks = NetClient.Common.request({
            url = "$SEARCH_API?aid=386088&q=${Uri.encodeUri(keyword)}"
            headers = defaultHeaders
        }) { json: JsonObject ->
            json.arr("result_groups").flatMap { group ->
                group.Object.arr("data").mapNotNull { it.ObjectNull }
            }
        }
        emitTracks(checkNotNull(tracks) { "汽水音乐关键词搜索暂不可用，请粘贴单曲或歌单分享链接" })
    }

    override fun findTrack(id: String) = flow {
        require(numericTrackId.matches(id)) { "汽水歌曲 ID 无效" }
        val track = publicTrack(requestTrack(id))?.let { summary(it, id) }
        emit([checkNotNull(track) { "未找到汽水音乐歌曲" }])
    }

    override fun parseLink(uri: Uri) = flow {
        require(matchesLink(uri)) { "请输入汽水音乐链接" }
        val target = if (uri.isMusicLinkOn("qishui.douyin.com") && uri.path.orEmpty().startsWith("/s/")) {
            val redirected = NetClient.Common.request<String, String>({
                url = uri.toString()
                headers = defaultHeaders
            }) { url }
            redirected?.let(Uri::parse)?.takeIf(::matchesLink) ?: error("无法解析汽水音乐短链接")
        } else uri
        val playlist = target.parameterId("playlist_id")?.takeIf(numericTrackId::matches)
        if (playlist != null) {
            val tracks = requestSharePage("$PLAYLIST_SHARE_PAGE?playlist_id=$playlist", "playlist_page")
                ?.get("medias").ArrayNull?.mapNotNull { it.ObjectNull }
            emitTracks(checkNotNull(tracks) { "读取汽水音乐歌单失败" })
        }
        else {
            val id = target.parameterId("track_id") ?: error("链接缺少有效单曲或歌单 ID")
            findTrack(id).collect { emit(it) }
        }
    }

    private fun convertLyrics(content: String): String = content.lineSequence().mapNotNull { line ->
        val match = lyricLine.find(line) ?: return@mapNotNull null
        val time = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
        val text = lyricTag.replace(match.groupValues[3], "")
        if (text.isBlank()) return@mapNotNull null
        val minutes = (time / 60000).toString().padStart(2, '0')
        val seconds = (time / 1000 % 60).toString().padStart(2, '0')
        val hundredths = (time / 10 % 100).toString().padStart(2, '0')
        "[$minutes:$seconds.$hundredths]$text"
    }.joinToString("\n")

    override suspend fun resolveDownload(track: PlatformMusicInfo): PlatformMusicResources? {
        if (!numericTrackId.matches(track.id)) return null
        val audio = requestSharePage("$TRACK_SHARE_PAGE?track_id=${track.id}", "track_page")
            ?.get("audioWithLyricsOption").ObjectNull ?: return null
        val audioUrl = audio["url"].StringNull
            ?.takeIf { it.isNotBlank() && !it.substringBefore('?').endsWith(".mp4", ignoreCase = true) } ?: return null
        val duration = audio["trackInfo"].ObjectNull?.get("duration").StringNull?.toLongOrNull()
        val previewStart = audio["previewStart"].StringNull?.toDoubleOrNull() ?: 0.0
        val previewEnd = audio["previewEnd"].StringNull?.toDoubleOrNull()
        if (previewStart > 0.0 || (duration != null && previewEnd != null && previewEnd > 0.0 && previewEnd * 1000 < duration - 1000)) return null

        val data = requestTrack(track.id)
        val cover = track.pic.ifEmpty { audio["coverURL"].StringNull.orEmpty() }
        val lyric = data?.get("lyric").ObjectNull?.get("content").StringNull.orEmpty()
        return PlatformMusicResources(audioUrl, cover, Lrc.parse(convertLyrics(lyric)).toString())
    }
}
