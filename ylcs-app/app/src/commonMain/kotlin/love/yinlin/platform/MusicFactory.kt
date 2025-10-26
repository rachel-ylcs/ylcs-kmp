package love.yinlin.platform

import androidx.compose.runtime.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import love.yinlin.common.Paths
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.catching
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import love.yinlin.service
import love.yinlin.service.PlatformContext

@Stable
abstract class MusicFactory {
    companion object {
        const val UPDATE_INTERVAL: Long = 150L
    }

    // 初始化
    abstract val isInit: Boolean

    private fun initLibrary() = Coroutines.startIO {
        val musicPath = Paths.musicPath
        SystemFileSystem.list(musicPath).map { it.name }.forEach { id ->
            catching {
                val configPath = Path(musicPath, id, MusicResourceType.Config.default.toString())
                val info = SystemFileSystem.source(configPath).buffered().use { it.readString().parseJsonValue<MusicInfo>() }!!
                musicLibrary[info.id] = info
            }
        }
    }

    private suspend fun initLastStatus() {
        if (isInit) {
            // 更新播放模式
            updatePlayMode(service.config.musicPlayMode)
            // 恢复上一次播放
            val playlistName = service.config.lastPlaylist
            if (playlistName.isNotEmpty()) {
                val playlist = service.config.playlistLibrary[playlistName]
                val musicName = service.config.lastMusic
                if (playlist != null) startPlaylist(playlist, musicName.ifEmpty { null }, false)
            }
        }
    }

    protected abstract suspend fun init()

    fun initFactory() {
        Coroutines.startCPU {
            Platform.useNot(Platform.WebWasm) { initLibrary() }
            init()
            if (isInit) initLastStatus()
        }
    }

    // 悬浮歌词
    var floatingLyrics: FloatingLyrics? by mutableRefStateOf(null)

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

    var currentPlaylist: MusicPlaylist? by mutableRefStateOf(null)
        protected set

    // 通用操作
    suspend fun updateMusicLibraryInfo(ids: List<String>) {
        for (id in ids) {
            val modification = musicLibrary[id]?.modification ?: 0
            val info = Coroutines.io {
                catchingNull {
                    val configPath = Path(Paths.musicPath, id, MusicResourceType.Config.default.toString())
                    SystemFileSystem.source(configPath).buffered().use { it.readString().parseJsonValue<MusicInfo>() }!!
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
        service.config.lastPlaylist = lastPlaylist
        if (lastPlaylist.isNotEmpty()) musicInfo?.let { service.config.lastMusic = it.id }
        else service.config.lastMusic = ""
    }

    protected fun onPlayModeChanged(mode: MusicPlayMode) {
        service.config.musicPlayMode = mode
    }

    protected fun onPlayerStop() {
        service.config.lastPlaylist = ""
        service.config.lastMusic = ""
    }
}

expect fun buildMusicFactory(context: PlatformContext): MusicFactory

@Stable
expect class MusicPlayer(context: PlatformContext) {
    val isInit: Boolean
    val isPlaying: Boolean
    val position: Long
    val duration: Long
    suspend fun init()
    suspend fun load(path: Path)
    fun play()
    fun pause()
    fun stop()
    fun release()
}