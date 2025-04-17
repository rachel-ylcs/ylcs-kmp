package love.yinlin.platform

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.utils.io.core.readText
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.parseJsonValue

@Stable
val MusicInfo.path get(): Path = Path(OS.Storage.musicPath, this.id)
@Stable
val MusicInfo.audioPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.Audio.defaultFilename)
@Stable
val MusicInfo.recordPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.Record.defaultFilename)
@Stable
val MusicInfo.backgroundPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.Background.defaultFilename)
@Stable
val MusicInfo.lyricsPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.LineLyrics.defaultFilename)

@Stable
abstract class MusicFactory {
    companion object {
        const val UPDATE_INTERVAL: Long = 150L
    }

    // 初始化
    abstract val isInit: Boolean
    abstract suspend fun init()

    // 当前状态
    abstract val error: Throwable?
    abstract val playMode: MusicPlayMode
    abstract val musicList: List<MusicInfo>
    abstract val isReady: Boolean
    abstract val isPlaying: Boolean
    abstract val currentPosition: Long
    abstract val currentDuration: Long

    // 接口
    abstract suspend fun updatePlayMode(musicPlayMode: MusicPlayMode)
    abstract suspend fun play()
    abstract suspend fun pause()
    abstract suspend fun stop()
    abstract suspend fun gotoPrevious()
    abstract suspend fun gotoNext()
    abstract suspend fun seekTo(position: Long)
    abstract suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?)
    abstract suspend fun addMedia(media: MusicInfo)
    abstract suspend fun removeMedia(index: Int)
    abstract suspend fun moveMedia(start: Int, end: Int)

    // 库
    val musicLibrary = mutableStateMapOf<String, MusicInfo>()

    suspend fun initLibrary() {
        Coroutines.io {
            val musicPath = OS.Storage.musicPath
            SystemFileSystem.list(musicPath).map { it.name }.forEach { id ->
                try {
                    val configPath = Path(musicPath, id, MusicResourceType.Config.defaultFilename)
                    val info = SystemFileSystem.source(configPath).buffered().use { it.readText().parseJsonValue<MusicInfo>() }!!
                    musicLibrary.put(info.id, info)
                }
                catch (_: Throwable) { }
            }
        }
    }

    var currentPlaylist: MusicPlaylist? by mutableStateOf(null)
        protected set
    var currentMusic: MusicInfo? by mutableStateOf(null)
        protected set

    // 通用操作
    suspend fun startPlaylist(playlist: MusicPlaylist, startId: String? = null) {
        if (isInit && currentPlaylist != playlist) {
            val actualMusicList = mutableListOf<MusicInfo>()
            for (id in playlist.items) {
                musicLibrary[id]?.let { actualMusicList += it }
            }
            stop()
            if (actualMusicList.isNotEmpty()) {
                currentPlaylist = playlist
                val index = if (startId != null) actualMusicList.indexOfFirst { it.id == startId } else -1
                prepareMedias(actualMusicList, if (index != -1) index else null)
                play()
            }
        }
    }

    suspend fun switchPlayMode() {
        if (isInit) updatePlayMode(playMode.next)
    }
}