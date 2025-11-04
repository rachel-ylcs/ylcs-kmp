package love.yinlin.startup

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import love.yinlin.AsyncStartup
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.StartupFetcher
import love.yinlin.app
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
import love.yinlin.platform.ioContext
import love.yinlin.platform.lyrics.FloatingLyrics
import love.yinlin.platform.lyrics.LrcLayout
import love.yinlin.platform.lyrics.LyricsEngine

@StartupFetcher(index = 0, name = "rootPath", returnType = Path::class)
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

    protected abstract suspend fun initController(context: Context)

    // 歌词引擎
    val lyrics = LrcLayout()
    var engine by mutableRefStateOf(LyricsEngine.Default)
    lateinit var floatingLyrics: FloatingLyrics
        private set

    // 公共
    protected lateinit var rootPath: Path
    val library = mutableStateMapOf<String, MusicInfo>()
    var playlist: MusicPlaylist? by mutableRefStateOf(null)
        protected set

    private suspend fun initLibrary() {
        Platform.useNot(Platform.WebWasm) {
            rootPath.list().map { it.name }.forEach { id ->
                val configPath = Path(rootPath, id, ModResourceType.Config.filename)
                val info = configPath.readText()?.parseJsonValue<MusicInfo>()
                if (info != null) library[info.id] = info
            }
        }
    }

    private suspend fun initLastStatus() {
        // 更新播放模式
        updatePlayMode(app.config.musicPlayMode)
        // 更新歌词引擎
        LyricsEngine[app.config.lyricsEngineType].let {
            if (engine != it) engine = it
        }
        // 恢复上一次播放
        app.config.playlistLibrary[app.config.lastPlaylist]?.let {
            startPlaylist(it, app.config.lastMusic.ifEmpty { null }, false)
        }
    }

    suspend fun updateMusicLibraryInfo(ids: List<String>) {
        Coroutines.io {
            for (id in ids) {
                val modification = library[id]?.modification ?: 0
                val configPath = Path(rootPath, id, ModResourceType.Config.filename)
                val info = configPath.readText()?.parseJsonValue<MusicInfo>()
                if (info != null) library[id] = info.copy(modification = modification + 1)
            }
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

    final override suspend fun init(context: Context, args: StartupArgs) {
        rootPath = args.fetch(0)
        floatingLyrics = FloatingLyrics()
        Coroutines.startCurrent {
            awaitAll(
                async(ioContext) { initLibrary() },
                async { initController(context) }
            )
            if (isInit) initLastStatus()
        }
    }

    override suspend fun initDelay(context: Context, args: StartupArgs) {
        Coroutines.startCurrent {
            launch { floatingLyrics.initDelay(context) }
        }
    }

    // 回调
    fun onMusicChanged(musicInfo: MusicInfo?) {
        val lastPlaylist = playlist?.name ?: ""
        app.config.lastPlaylist = lastPlaylist
        if (lastPlaylist.isNotEmpty()) musicInfo?.let { app.config.lastMusic = it.id }
        else app.config.lastMusic = ""
    }

    fun onPlayModeChanged(mode: MusicPlayMode) {
        app.config.musicPlayMode = mode
    }

    fun onPlayerStop() {
        app.config.lastPlaylist = ""
        app.config.lastMusic = ""
    }
}

@StartupFetcher(index = 0, name = "rootPath", returnType = Path::class)
expect fun buildMusicPlayer() : StartupMusicPlayer