package love.yinlin.tpl

import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import love.yinlin.extension.catchingError
import love.yinlin.foundation.NetClient
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.tpl.music.PlatformMusicAPI
import love.yinlin.tpl.music.PlatformMusicResolvers
import love.yinlin.tpl.music.SodaMusicAPI
import love.yinlin.uri.Uri
import kotlin.test.Test

class TestPlatformMusic {
    @Test
    fun identifyLinks() {
        val samples = listOf(
            "分享歌曲 https://y.qq.com/n/ryqq/songDetail/003yJ3Ba1bDVJc。",
            "https://music.163.com/#/song?textid=1064008&id=504686858",
            "https://www.kugou.com/song/#hash=1234567890abcdef1234567890abcdef",
            "https://qishui.douyin.com/s/ix7xxWf3/",
            "https://c.migu.cn/playlist",
            "003yJ3Ba1bDVJc",
            "504686858",
            "7145746290255595521",
            "https://music.163.com.example.org/song?id=504686858",
            "银临 腐草为萤"
        )
        for (sample in samples) {
            println("$sample -> ${PlatformMusicResolvers.identify(sample)?.type?.description ?: "待选平台"}")
        }
    }

    @Test
    fun inspectLiveSearchAndDownload() {
        if (System.getenv("RACHEL_PLATFORM_MUSIC_LIVE_TEST") != "1") {
            println("设置 RACHEL_PLATFORM_MUSIC_LIVE_TEST=1 可检查真实平台请求")
            return
        }

        runBlocking {
            PlatformMusicResolvers.all.map { resolver ->
                launch {
                    val platform = resolver.type.description
                    val completed = withTimeoutOrNull(25_000) {
                        catchingError {
                            val batch = resolver.search("银临 腐草为萤").firstOrNull().orEmpty()
                            println("[$platform] 首批 ${batch.size} 首，样本 ${batch.take(3).map { it.name to it.singer }}")
                            batch.firstOrNull()?.let { inspectResources(resolver, it) }
                        }?.let { println("[$platform] ${it.message}") }
                        true
                    }
                    if (completed == null) println("[$platform] 请求超时")
                }
            }.joinAll()
        }
    }

    @Test
    fun inspectSodaPublicLinks() {
        if (System.getenv("RACHEL_PLATFORM_MUSIC_LIVE_TEST") != "1") return

        runBlocking {
            val completed = withTimeoutOrNull(25_000) {
                catchingError {
                    val track = SodaMusicAPI.findTrack("7145746290255595521").firstOrNull()?.firstOrNull()
                    println("[汽水音乐] 公开单曲 ${track?.name}，歌手 ${track?.singer}")
                    if (track != null) inspectResources(SodaMusicAPI, track)
                }?.let { println("[汽水音乐] 单曲失败 ${it.message}") }

                catchingError {
                    val playlist = Uri.parse("https://music.douyin.com/qishui/share/playlist?playlist_id=7203983736064622592")!!
                    val batch = SodaMusicAPI.parseLink(playlist).firstOrNull().orEmpty()
                    println("[汽水音乐] 公开歌单首批 ${batch.size} 首")
                }?.let { println("[汽水音乐] 歌单失败 ${it.message}") }
                true
            }
            if (completed == null) println("[汽水音乐] 分享链接诊断超时")
        }
    }

    private suspend fun inspectResources(resolver: PlatformMusicAPI, track: PlatformMusicInfo) {
        val platform = resolver.type.description
        val resources = resolver.resolveDownload(track)
        println("[$platform] 音源链接 ${!resources?.audioUrl.isNullOrBlank()}，封面链接 ${!resources?.coverUrl.isNullOrBlank()}，歌词 ${resources?.lyrics?.length ?: 0} 字符")
        if (resources != null) {
            val status = NetClient.File.request<String, Int>({
                url = resources.audioUrl
                method = HttpMethod.Head
            }) { status.value }
            println("[$platform] 音源 HEAD 状态 ${status ?: "无响应"}")
        }
    }
}
