package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.music.SodaMusicInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.http.NetHeader
import love.yinlin.tpl.music.PlatformMusicRule.Kind
import love.yinlin.uri.Uri

@Stable
object SodaMusicManager : PlatformMusicManager(PlatformMusicType.Soda) {
    private const val SEARCH_API = "https://api.qishui.com/luna/pc/sug"
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

    override val rules: List<PlatformMusicRule> = [
        // 短链接-> https://qishui.douyin.com/s/ix7xxWf3/
        PlatformMusicRule.redirect("短链接", "qishui.douyin.com") { it.path.orEmpty().startsWith("/s/") },
        // 长链接 playlist_id -> https://music.douyin.com/qishui/share/playlist?playlist_id=7635277649740922923&...
        musicLink("歌单", Kind.Playlist, "playlist_id"),
        // 长链接 track_id -> https://music.douyin.com/qishui/share/track?track_id=7145746290255595521
        musicLink("单曲", Kind.Track, "track_id"),
        // 纯数字作为单曲 ID -> 7145746290255595521
        PlatformMusicRule.trackId("单曲ID", Regex("[0-9]{19}")),
    ]

    private fun musicLink(description: String, kind: Kind, parameter: String): PlatformMusicRule = PlatformMusicRule.link(description, kind, "music.douyin.com", "qishui.douyin.com", "qishui.com") {
        it.parameterId(parameter)?.takeIf(numericTrackId::matches)
    }

    override val redirectHeaders: NetHeader get() = defaultHeaders

    private fun summary(track: JsonObject, id: String): PlatformMusicInfo? {
        val name = track["name"].StringNull ?: return null
        val singer = track["artists"].ArrayNull.orEmpty().mapNotNull { it.ObjectNull?.get("name").StringNull }.joinToString(",")
        val cover = track["album"].ObjectNull?.get("url_cover").ObjectEmpty
        val url = cover["urls"].ArrayNull?.firstOrNull().StringNull
        val uri = cover["uri"].StringNull
        val pic = if (url != null && uri != null) "$url$uri~c5_375x375.jpg" else ""
        return SodaMusicInfo(id, name, singer, pic)
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

    // 有摘要的条目先显示，缺失摘要的条目再逐首补查
    private suspend fun appendTracks(rows: List<JsonObject>, results: MutableList<PlatformMusicInfo>) {
        val ready: MutableList<PlatformMusicInfo> = []
        val pending = mutableSetOf<String>()
        val readyIds = mutableSetOf<String>()
        for (row in rows) {
            val entity = row["entity"].ObjectNull ?: row
            val track = entity["track"].ObjectNull ?: entity["track_wrapper"].ObjectNull?.get("track").ObjectNull ?: entity
            val id = (track["id"].StringNull ?: row["id"].StringNull)?.takeIf(numericTrackId::matches) ?: continue
            val info = summary(track, id)
            if (info != null) {
                if (readyIds.add(id)) ready += info
            }
            else pending += id
        }
        if (ready.isNotEmpty()) {
            pending.removeAll(readyIds)
            results.addAll(ready)
        }

        var failures = 0
        for (id in pending) {
            val info = publicTrack(requestTrack(id))?.let { summary(it, id) }
            if (info != null) results.add(info)
            else failures++
        }
        check(failures == 0) { if (ready.isEmpty() && failures == pending.size) "读取汽水音乐歌曲失败" else "部分汽水音乐歌曲读取失败" }
    }

    override suspend fun searchKeyword(keyword: String, results: MutableList<PlatformMusicInfo>) {
        val tracks = NetClient.Common.request({
            url = "$SEARCH_API?aid=386088&q=${Uri.encodeUri(keyword)}"
            headers = defaultHeaders
        }) { json: JsonObject ->
            check(json["status_code"].StringNull.let { it == null || it == "0" })
            json["sugs"].ArrayNull.orEmpty().mapNotNull { item ->
                item.ObjectNull?.takeIf { it["content_type"].StringNull == "track" }
            }
        }
        appendTracks(checkNotNull(tracks) { "汽水音乐关键词搜索失败" }, results)
    }

    override suspend fun findTrack(id: String): PlatformMusicInfo? = publicTrack(requestTrack(id))?.let { summary(it, id) }

    override suspend fun findPlaylist(id: String, results: MutableList<PlatformMusicInfo>) {
        val tracks = requestSharePage("$PLAYLIST_SHARE_PAGE?playlist_id=$id", "playlist_page")?.get("medias").ArrayNull?.mapNotNull { it.ObjectNull }
        appendTracks(checkNotNull(tracks) { "读取汽水音乐歌单失败" }, results)
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

    override suspend fun resolveDownload(track: PlatformMusicInfo): Download? {
        if (!numericTrackId.matches(track.id)) return null

        val audio = requestSharePage("$TRACK_SHARE_PAGE?track_id=${track.id}", "track_page")?.get("audioWithLyricsOption").ObjectNull ?: return null
        if (audio["group_download_level"].StringNull == "pay") return null
        val audioUrl = audio["url"].StringNull?.takeIf { it.isNotBlank() && !it.substringBefore('?').endsWith(".mp4", ignoreCase = true) } ?: return null
        val duration = audio["trackInfo"].ObjectNull?.get("duration").StringNull?.toLongOrNull()
        val previewStart = audio["previewStart"].StringNull?.toDoubleOrNull() ?: 0.0
        val previewEnd = audio["previewEnd"].StringNull?.toDoubleOrNull()
        if (previewStart > 0.0 || (duration != null && previewEnd != null && previewEnd > 0.0 && previewEnd * 1000 < duration - 1000)) return null

        val data = requestTrack(track.id)
        val cover = track.pic.ifEmpty { audio["coverURL"].StringNull.orEmpty() }
        val lyric = data?.get("lyric").ObjectNull?.get("content").StringNull.orEmpty()

        return Download(audioUrl, cover, convertLyrics(lyric))
    }
}
