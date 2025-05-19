package love.yinlin.platform

import androidx.compose.runtime.*
import io.ktor.utils.io.core.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.parseJsonValue

@Stable
abstract class MusicFactory {
    companion object {
        const val UPDATE_INTERVAL: Long = 150L
    }

    // 初始化
    protected abstract val isInit: Boolean

    private fun initLibrary() = Coroutines.startIO {
        val musicPath = OS.Storage.musicPath
        SystemFileSystem.list(musicPath).map { it.name }.forEach { id ->
            try {
                val configPath = Path(musicPath, id, MusicResourceType.Config.default.toString())
                val info = SystemFileSystem.source(configPath).buffered().use { it.readText().parseJsonValue<MusicInfo>() }!!
                musicLibrary[info.id] = info
            }
            catch (_: Throwable) { }
        }
    }

    private suspend fun initLastStatus() {
        if (isInit) {
            // 更新播放模式
            updatePlayMode(app.config.musicPlayMode)
            // 恢复上一次播放
            val playlistName = app.config.lastPlaylist
            if (playlistName.isNotEmpty()) {
                val playlist = app.config.playlistLibrary[playlistName]
                val musicName = app.config.lastMusic
                if (playlist != null) startPlaylist(playlist, musicName.ifEmpty { null }, false)
            }
        }
    }

    protected abstract suspend fun init()

    fun initFactory() {
        Coroutines.startCPU {
            OS.ifNotPlatform(Platform.WebWasm) { initLibrary() }
            init()
            if (isInit) initLastStatus()
        }
    }

    // 悬浮歌词
    var floatingLyrics: FloatingLyrics? by mutableStateOf(null)

    // 当前状态
    abstract val error: Throwable?
    abstract val playMode: MusicPlayMode
    abstract val musicList: List<MusicInfo>
    abstract val isReady: Boolean
    abstract val isPlaying: Boolean
    abstract val currentPosition: Long
    abstract val currentDuration: Long
    abstract val currentMusic: MusicInfo?

    // 接口
    abstract suspend fun updatePlayMode(musicPlayMode: MusicPlayMode)
    abstract suspend fun play()
    abstract suspend fun pause()
    abstract suspend fun stop()
    abstract suspend fun gotoPrevious()
    abstract suspend fun gotoNext()
    abstract suspend fun gotoIndex(index: Int)
    abstract suspend fun seekTo(position: Long)
    abstract suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?, playing: Boolean)
    abstract suspend fun addMedias(medias: List<MusicInfo>)
    abstract suspend fun removeMedia(index: Int)

    // 库
    val musicLibrary = mutableStateMapOf<String, MusicInfo>()

    var currentPlaylist: MusicPlaylist? by mutableStateOf(null)
        protected set

    // 通用操作
    suspend fun updateMusicLibraryInfo(ids: List<String>) {
        for (id in ids) {
            val modification = musicLibrary[id]?.modification ?: 0
            val info = Coroutines.io {
                try {
                    val configPath = Path(OS.Storage.musicPath, id, MusicResourceType.Config.default.toString())
                    SystemFileSystem.source(configPath).buffered().use { it.readText().parseJsonValue<MusicInfo>() }!!
                }
                catch (_: Throwable) {
                    null
                }
            }
            if (info != null) musicLibrary[id] = info.copy(modification = modification + 1)
        }
    }

    suspend fun startPlaylist(playlist: MusicPlaylist, startId: String? = null, playing: Boolean) {
        if (isInit && currentPlaylist != playlist) {
            val actualMusicList = mutableListOf<MusicInfo>()
            for (id in playlist.items) {
                musicLibrary[id]?.let { actualMusicList += it }
            }
            stop()
            if (actualMusicList.isNotEmpty()) {
                currentPlaylist = playlist
                val index = if (startId != null) actualMusicList.indexOfFirst { it.id == startId } else -1
                prepareMedias(actualMusicList, if (index != -1) index else null, playing)
            }
        }
    }

    suspend fun switchPlayMode() {
        if (isInit) {
            val nextMode = playMode.next
            updatePlayMode(nextMode)
        }
    }

    // 回调
    protected fun onMusicChanged(musicInfo: MusicInfo?) {
        val lastPlaylist = currentPlaylist?.name ?: ""
        app.config.lastPlaylist = lastPlaylist
        if (lastPlaylist.isNotEmpty()) musicInfo?.let { app.config.lastMusic = it.id }
        else app.config.lastMusic = ""
    }

    protected fun onPlayModeChanged(mode: MusicPlayMode) {
        app.config.musicPlayMode = mode
    }

    protected fun onPlayerStop() {
        app.config.lastPlaylist = ""
        app.config.lastMusic = ""
    }
}