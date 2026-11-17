package love.yinlin.screen

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.writeString
import love.yinlin.app
import love.yinlin.common.DataBin
import love.yinlin.compose.ds.DataSourceMusic
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.extension.catchingError
import love.yinlin.extension.toJsonString
import love.yinlin.foundation.NetClient
import love.yinlin.fs.File
import love.yinlin.startup.StartupMusicPlayer
import love.yinlin.tpl.music.PlatformMusicAPI
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import kotlin.uuid.Uuid

internal object PlatformMusicModImporter {
    private val libraryLock = Mutex()
    private val validId = Regex("[A-Za-z0-9]{1,80}")
    private val resources = ModResourceType.BASE.filter { it != ModResourceType.Config } + ModResourceType.Config

    suspend fun importTrack(player: StartupMusicPlayer, resolver: PlatformMusicAPI, track: PlatformMusicInfo, id: String) {
        require(validId.matches(id)) { "歌曲 ID 无效" }
        require(id != player.currentId) { "请先停止播放「${track.name}」" }
        val source = checkNotNull(resolver.resolveDownload(track)) { "平台未提供可下载的音源" }

        val staging = File(app.cachePath, "platform-music-${Uuid.random()}")
        try {
            staging.mkdir()
            download(source.audioUrl, File(staging, ModResourceType.Audio.filename), "音频")
            download(source.coverUrl, File(staging, ModResourceType.Record.filename), "封面")
            File(staging, ModResourceType.Background.filename).write { it.write(DataBin.BlackBackgroundPicture) }
            val lyrics = source.lyrics.ifBlank { "[offset:0]\n" }
            File(staging, ModResourceType.LineLyrics.filename).write { it.writeString(lyrics) }

            val info = MusicInfo(
                version = "1.0",
                author = resolver.type.description,
                id = id,
                name = track.name,
                singer = track.singer.ifBlank { "未知" },
                lyricist = "未知",
                composer = "未知",
                album = "未知",
                chorus = null
            )
            File(staging, ModResourceType.Config.filename).write { it.writeString(info.toJsonString()) }

            libraryLock.withLock {
                require(id != player.currentId) { "请先停止播放「${track.name}」" }
                val reload = if (id in player.musicList) StartupMusicPlayer.ReloadAddData.Replace else player.checkReloadPlaylistByAdd(id)
                withContext(NonCancellable) {
                    publish(staging, File(app.modPath, id))
                    val saved = checkNotNull(DataSourceMusic.reloadMusicInfo(id)) { "读取已导入的歌曲失败" }
                    DataSourceMusic.library[id] = saved
                    player.reloadPlaylistByAdd(id, reload)
                }
            }
        }
        finally {
            withContext(NonCancellable) { staging.deleteRecursively() }
        }
    }

    private suspend fun download(url: String, file: File, label: String) {
        val uri = Uri.parse(url)
        require(uri != null && (uri.scheme == Scheme.Http || uri.scheme == Scheme.Https) && !uri.host.isNullOrBlank()) { "平台未提供有效的${label}链接" }
        file.write { check(NetClient.File.download(url, it)) { "下载${label}失败" } }
    }

    private suspend fun copy(source: File, target: File) {
        source.read { input -> target.write { input.transferTo(it) } }
    }

    private suspend fun publish(staging: File, destination: File) {
        val existing = destination.exists()
        val backup = File(staging, "backup")
        val saved = mutableSetOf<ModResourceType>()
        if (existing) {
            backup.mkdir()
            for (type in resources) {
                val file = File(destination, type.filename)
                if (file.isFile()) {
                    copy(file, File(backup, type.filename))
                    saved += type
                }
            }
        }

        val failure = catchingError {
            destination.mkdir()
            for (type in resources) copy(File(staging, type.filename), File(destination, type.filename))
        } ?: return

        if (!existing) destination.deleteRecursively()
        else {
            for (type in resources) {
                val file = File(destination, type.filename)
                if (type in saved) copy(File(backup, type.filename), file)
                else file.delete()
            }
        }
        throw failure
    }
}
