package love.yinlin.startup

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.io.files.Path
import love.yinlin.AsyncStartup
import love.yinlin.Context
import love.yinlin.StartupArg
import love.yinlin.StartupArgs
import love.yinlin.StartupFetcher
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.extension.list
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.readText
import love.yinlin.platform.Coroutines
import love.yinlin.platform.Platform

@StartupFetcher(index = 0, name = "rootPath", returnType = Path::class)
@StartupArg(index = 1, name = "listener", type = StartupMusicPlayer.Listener::class)
abstract class StartupMusicPlayer : AsyncStartup {
    companion object {
        const val PROGRESS_UPDATE_INTERVAL = 150L
    }

    // 接口
    abstract val isInit: Boolean
    abstract val error: Throwable?
    abstract val playMode: MusicPlayMode
    abstract val musicList: List<MusicInfo>
    abstract val isReady: Boolean
    abstract val isPlaying: Boolean
    abstract val currentDuration: Long
    abstract val currentPosition: Long
    abstract val currentMusic: MusicInfo?

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

    protected abstract suspend fun initController()

    // 公共
    protected lateinit var rootPath: Path
    val library = mutableStateMapOf<String, MusicInfo>()
    var playlist: MusicPlaylist? by mutableRefStateOf(null)
        protected set

    private suspend fun initLibrary() = Coroutines.io {
        Platform.useNot(Platform.WebWasm) {
            rootPath.list().map { it.name }.forEach { id ->
                val configPath = Path(rootPath, id, ModResourceType.Config.filename)
                val info = configPath.readText()?.parseJsonValue<MusicInfo>()
                if (info != null) library[info.id] = info
            }
        }
    }

    @Stable
    data class LastStatus(
        val playMode: MusicPlayMode = MusicPlayMode.DEFAULT,
        val playlist: MusicPlaylist? = null,
        val musicId: String = "",
    )

    private suspend fun initLastStatus(lastStatus: LastStatus) {
        // 更新播放模式
        updatePlayMode(lastStatus.playMode)
        // 恢复上一次播放
        lastStatus.playlist?.let {
            startPlaylist(it, lastStatus.musicId.ifEmpty { null }, false)
        }
    }

    suspend fun updateMusicLibraryInfo(ids: List<String>) {
        for (id in ids) {
            val modification = library[id]?.modification ?: 0
            val info = Coroutines.io {
                val configPath = Path(rootPath, id, ModResourceType.Config.filename)
                configPath.readText()?.parseJsonValue<MusicInfo>()
            }
            if (info != null) library[id] = info.copy(modification = modification + 1)
        }
    }

    suspend fun startPlaylist(playlist: MusicPlaylist, startId: String? = null, playing: Boolean) {
        if (isInit && this.playlist != playlist) {
            val actualMusicList = mutableListOf<MusicInfo>()
            for (id in playlist.items) {
                library[id]?.let { actualMusicList += it }
            }
            stop()
            if (actualMusicList.isNotEmpty()) {
                this.playlist = playlist
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

    protected fun MusicInfo.path(type: ModResourceType) = this.path(rootPath, type)

    // 回调
    interface Listener {
        fun StartupMusicPlayer.onMusicChanged(musicInfo: MusicInfo?) { }
        fun StartupMusicPlayer.onPlayModeChanged(mode: MusicPlayMode) { }
        fun StartupMusicPlayer.onPlayerStop() { }
        val onLastStatusResume: LastStatus get() = LastStatus()
    }

    protected var listener: Listener = object : Listener {}

    final override suspend fun init(context: Context, args: StartupArgs) {
        rootPath = args.fetch(0)
        listener = args[1]
        Coroutines.startCurrent {
            awaitAll(
                async { initLibrary() },
                async { initController() },
            )
            if (isInit) {
                initLastStatus(with(listener) { onLastStatusResume })
            }
        }
    }
}

@StartupFetcher(index = 0, name = "rootPath", returnType = Path::class)
@StartupArg(index = 1, name = "listener", type = StartupMusicPlayer.Listener::class)
expect fun buildMusicPlayer() : StartupMusicPlayer