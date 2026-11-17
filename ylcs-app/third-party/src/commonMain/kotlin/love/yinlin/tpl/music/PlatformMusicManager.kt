package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import love.yinlin.concurrent.Mutex
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.MainCoroutine
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.encoding.convert
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import love.yinlin.extension.toJsonString
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.http.NetHeader
import love.yinlin.foundation.parser.lrc.Lrc
import love.yinlin.fs.File
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import kotlin.uuid.Uuid

@Stable
abstract class PlatformMusicManager(val type: PlatformMusicType) {
    abstract val rules: List<PlatformMusicRule>

    protected data class Download(val audioUrl: String, val coverUrl: String, val lyrics: String)

    @Stable
    data class Query(val keyword: String, val match: PlatformMusicRule.Match?)

    @Stable
    data class Recognition(val manager: PlatformMusicManager, val query: Query)

    companion object {
        val All: List<PlatformMusicManager> = [QQMusicManager, NetEaseCloudManager, KugouMusicManager, SodaMusicManager, MiguMusicManager]

        private val importLock = Mutex()
        private val validModId = Regex("[A-Za-z0-9]{1,80}")
        private val modResources = ModResourceType.BASE.filter { it != ModResourceType.Config } + ModResourceType.Config

        operator fun get(type: PlatformMusicType): PlatformMusicManager = All.first { it.type == type }

        fun recognize(value: String): Recognition? {
            val input = PlatformMusicRule.Input(value)
            return All.firstNotNullOfOrNull { manager ->
                manager.match(input)?.let { Recognition(manager, Query("", it)) }
            }
        }
    }

    protected val numericTrackId = Regex("[0-9]{1,19}")

    protected fun Uri.parameterId(name: String): String? = params[name]?.substringBefore('#')

    protected fun Uri.lastPathSegment(): String? = path?.trimEnd('/')?.substringAfterLast('/')?.takeIf(String::isNotEmpty)

    private fun match(input: PlatformMusicRule.Input, redirects: Boolean = true): PlatformMusicRule.Match? = rules.firstNotNullOfOrNull { rule ->
        if (redirects || rule.kind != PlatformMusicRule.Kind.Redirect) rule.match(input) else null
    }

    fun query(value: String): Query? {
        val input = PlatformMusicRule.Input(value)
        val matched = match(input)

        return when {
            matched != null -> Query("", matched)
            input.text.isNotEmpty() && input.links.isEmpty() -> Query(input.text, null)
            else -> null
        }
    }

    @MainCoroutine
    suspend fun search(query: Query, results: MutableList<PlatformMusicInfo>) {
        val matched = query.match
        if (matched == null) {
            searchKeyword(query.keyword, results)
            return
        }

        require(matched.rule in rules) { "匹配规则与平台不一致" }

        (val rule, val value) = if (matched.rule.kind == PlatformMusicRule.Kind.Redirect) {
            val target = checkNotNull(resolveRedirect(matched.value)) { "无法解析${type.description}短链接" }
            checkNotNull(match(PlatformMusicRule.Input(target), redirects = false)) { "短链接未指向可用的${type.description}资源" }
        } else matched

        when (rule.kind) {
            PlatformMusicRule.Kind.Track -> results += checkNotNull(findTrack(value)) { "未找到${type.description}歌曲" }
            PlatformMusicRule.Kind.Playlist -> findPlaylist(value, results)
            PlatformMusicRule.Kind.Album -> findAlbum(value, results)
            PlatformMusicRule.Kind.Redirect -> error("无法解析${type.description}短链接")
        }
    }

    protected open val redirectHeaders: NetHeader get() = NetHeader.Empty

    protected open suspend fun resolveRedirect(link: String): Uri? = NetClient.Common.request<String, Uri>({
        url = link
        headers = redirectHeaders
    }) {
        Uri.parse(url) ?: error("短链接地址无效")
    }

    protected abstract suspend fun searchKeyword(keyword: String, results: MutableList<PlatformMusicInfo>)

    protected abstract suspend fun findTrack(id: String): PlatformMusicInfo?

    protected open suspend fun findPlaylist(id: String, results: MutableList<PlatformMusicInfo>): Unit = error("当前平台不支持歌单")

    protected open suspend fun findAlbum(id: String, results: MutableList<PlatformMusicInfo>): Unit = error("当前平台不支持专辑")

    protected fun MutableList<PlatformMusicInfo>.addBatch(tracks: List<PlatformMusicInfo>?, resource: String) {
        addAll(checkNotNull(tracks) { "读取${type.description}${resource}失败" }.distinctBy { it.id })
    }

    protected abstract suspend fun resolveDownload(track: PlatformMusicInfo): Download?

    open fun modId(track: PlatformMusicInfo): String = type.prefix + track.id

    suspend fun importTrack(track: PlatformMusicInfo, cachePath: File, modPath: File, background: ByteArray): MusicInfo? {
        val id = modId(track)
        require(validModId.matches(id)) { "歌曲 ID 无效" }

        val destination = File(modPath, id)
        if (destination.exists()) return null

        (val audioUrl, val coverUrl, val lyrics) = checkNotNull(resolveDownload(track)) { "平台未提供可下载的音源" }
        val lrc = Lrc.parse(lyrics.removePrefix("\uFEFF"))
        require(lrc.isNotEmpty) { "平台未提供有效的歌词" }

        val info = MusicInfo(
            version = "1.0",
            author = type.description,
            id = id,
            name = track.name,
            singer = track.singer.ifBlank { "未知" },
            lyricist = "未知",
            composer = "未知",
            album = "未知",
            chorus = null,
            modification = 1,
        )

        val staging = File(cachePath, "platform-music-${Uuid.random()}")

        cleaning({
            staging.mkdir()
            download(audioUrl, File(staging, ModResourceType.Audio.filename), "音频")
            download(coverUrl, File(staging, ModResourceType.Record.filename), "封面")
            File(staging, ModResourceType.Background.filename).write { it.write(background) }
            File(staging, ModResourceType.LineLyrics.filename).write { it.write(lrc.toString().convert()) }
            File(staging, ModResourceType.Config.filename).write { it.write(info.toJsonString().convert()) }

            return importLock.with {
                if (destination.exists()) null
                else {
                    Coroutines.wait { publish(staging, destination) }
                    info
                }
            }
        }) {
            Coroutines.wait {
                staging.deleteRecursively()
            }
        }
    }

    private suspend fun download(url: String, file: File, label: String) {
        val uri = Uri.parse(url)
        require(uri != null && (uri.scheme == Scheme.Http || uri.scheme == Scheme.Https) && !uri.host.isNullOrBlank()) { "平台未提供有效的${label}链接" }
        file.write { sink ->
            check(NetClient.File.download(url, sink)) { "下载${label}失败" }
        }
    }

    private suspend fun publish(staging: File, destination: File) {
        val failure = catchingError {
            destination.mkdir()
            for (resource in modResources) {
                val name = resource.filename
                File(staging, name).read { source ->
                    File(destination, name).write { sink ->
                        source.transferTo(sink)
                    }
                }
            }
        }

        if (failure != null) {
            destination.deleteRecursively()
            throw failure
        }
    }
}
