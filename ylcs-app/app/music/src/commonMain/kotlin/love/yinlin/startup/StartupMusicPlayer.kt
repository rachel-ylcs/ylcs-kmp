package love.yinlin.startup

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import love.yinlin.app
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.ioContext
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.extension.list
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.readText
import love.yinlin.foundation.AsyncStartup
import love.yinlin.foundation.Context
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.StartupFetcher
import love.yinlin.media.MediaMetadataFetcher
import love.yinlin.media.MusicPlayerListener
import love.yinlin.media.buildMusicPlayer
import love.yinlin.media.lyrics.FloatingLyrics
import love.yinlin.media.lyrics.LyricsEngine
import love.yinlin.media.lyrics.LyricsEngineHost

@StartupFetcher(index = 0, name = "rootPath", returnType = Path::class)
@Stable
class StartupMusicPlayer : AsyncStartup() {
    private lateinit var rootPath: Path
    private fun MusicInfo.path(type: ModResourceType) = this.path(rootPath, type)

    // 外部数据提取器
    val fetcher = object : MediaMetadataFetcher<MusicInfo> {
        override val audioFocus: Boolean get() = app.config.audioFocus
        override val interval: Long get() = engine.interval

        override val MusicInfo.audioUri: String get() = this.path(ModResourceType.Audio).toString()
        override val MusicInfo.coverUri: String get() = this.path(ModResourceType.Record).toString()

        override val androidMusicServiceComponentName: Pair<String, String> = "love.yinlin" to "love.yinlin.MusicService"
    }

    // 数据仓库
    var playlist: MusicPlaylist? by mutableRefStateOf(null)
        private set
    val library = mutableStateMapOf<String, MusicInfo>()

    // 回调监听器
    val listener = object : MusicPlayerListener<MusicInfo> {
        override fun onMusicChanged(info: MusicInfo?) {
            val lastPlaylist = playlist?.name ?: ""
            app.config.lastPlaylist = lastPlaylist
            if (lastPlaylist.isNotEmpty()) info?.let { app.config.lastMusic = it.id }
            else app.config.lastMusic = ""
        }

        override fun onPlayModeChanged(mode: MediaPlayMode) {
            app.config.musicPlayMode = mode
        }

        override fun onPlayerStop() {
            playlist = null
            app.config.lastPlaylist = ""
            app.config.lastMusic = ""
        }
    }

    // 媒体控制器
    private val controller = buildMusicPlayer(fetcher)

    val isInit: Boolean get() = controller.isInit
    val isReady: Boolean get() = controller.isReady
    val isPlaying: Boolean get() = controller.isPlaying
    val playMode get() = controller.playMode
    val position: Long get() = controller.position
    val duration: Long get() = controller.duration
    val musicList: List<MusicInfo> get() = controller.musicList
    val music: MusicInfo? get() = controller.music
    val error: Throwable? get() = controller.error
    suspend fun play() = controller.play()
    suspend fun pause() = controller.pause()
    suspend fun stop() = controller.stop()
    suspend fun gotoPrevious() = controller.gotoPrevious()
    suspend fun gotoNext() = controller.gotoNext()
    suspend fun gotoIndex(index: Int) = controller.gotoIndex(index)
    suspend fun seekTo(position: Long) = controller.seekTo(position)
    suspend fun addMedias(medias: List<MusicInfo>) = controller.addMedias(medias)
    suspend fun removeMedia(index: Int) = controller.removeMedia(index)

    // 歌词引擎
    val engineHost = LyricsEngineHost { controller.seekTo(it) }
    var engine by mutableRefStateOf(LyricsEngine.Default)
    val floatingLyrics: FloatingLyrics = FloatingLyrics()

    private suspend fun initLibrary() {
        rootPath.list().map { it.name }.forEach { id ->
            val configPath = Path(rootPath, id, ModResourceType.Config.filename)
            val info = catchingNull { configPath.readText()!!.parseJsonValue<MusicInfo>() }
            if (info != null) library[info.id] = info
        }
    }

    private suspend fun initLastStatus() {
        // 更新播放模式
        controller.updatePlayMode(app.config.musicPlayMode)
        // 更新歌词引擎
        catchingError {
            val firstEngine = LyricsEngine[app.config.lyricsEngineOrder.first()]
            if (engine != firstEngine) engine = firstEngine
        }?.let { engine = LyricsEngine.Default }
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
                val info = catchingNull { configPath.readText()!!.parseJsonValue<MusicInfo>() }
                if (info != null) library[id] = info.copy(modification = modification + 1)
            }
        }
    }

    suspend fun startPlaylist(playlist: MusicPlaylist, startId: String? = null, playing: Boolean) {
        if (controller.isInit && this.playlist != playlist) {
            val actualMusicList = mutableListOf<MusicInfo>()
            for (id in playlist.items) {
                library[id]?.let { actualMusicList += it }
            }
            controller.stop()
            if (actualMusicList.isNotEmpty()) {
                this.playlist = playlist
                val index = if (startId != null) actualMusicList.indexOfFirst { it.id == startId } else -1
                controller.prepareMedias(actualMusicList, if (index != -1) index else null, playing)
            }
        }
    }

    suspend fun switchPlayMode() {
        if (controller.isInit) controller.updatePlayMode(controller.playMode.next)
    }

    override suspend fun init(scope: CoroutineScope, context: Context, args: StartupArgs) {
        args.fetch<Path?>(0)?.let {
            rootPath = it
            awaitAll(
                scope.async(ioContext) { initLibrary() },
                scope.async { controller.init(context) }
            )
            if (controller.isInit) {
                controller.listener = listener
                initLastStatus()
            }
        }
    }

    override suspend fun initLater(scope: CoroutineScope, context: Context, args: StartupArgs) {
        scope.launch { floatingLyrics.initDelay(context) }
    }

    override fun destroy(context: Context, args: StartupArgs) {
        controller.listener = null
        controller.release()
    }
}