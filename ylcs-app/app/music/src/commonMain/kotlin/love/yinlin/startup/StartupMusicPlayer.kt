package love.yinlin.startup

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import love.yinlin.app
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import love.yinlin.foundation.AsyncStartup
import love.yinlin.foundation.Context
import love.yinlin.foundation.StartupArgs
import love.yinlin.fs.File
import love.yinlin.media.MediaMetadataFetcher
import love.yinlin.media.MusicPlayerListener
import love.yinlin.media.buildMusicPlayer
import love.yinlin.media.lyrics.FloatingLyrics
import love.yinlin.media.lyrics.LyricsEngine
import love.yinlin.media.lyrics.LyricsEngineHost

@Stable
class StartupMusicPlayer : AsyncStartup() {
    private fun MusicInfo.path(type: ModResourceType) = this.path(app.modPath, type)

    // 外部数据提取器
    val fetcher = object : MediaMetadataFetcher {
        override val audioFocus: Boolean get() = app.config.audioFocus
        override val interval: Long get() = engine.interval

        override fun extractAudioUri(id: String): String? = library[id]?.path(ModResourceType.Audio)?.path
        override fun extractCoverUri(id: String): String? = library[id]?.path(ModResourceType.Record)?.path
        override fun extractMetadata(id: String): MediaInfo? = library[id]

        override val androidMusicServiceComponentName: Pair<String, String> = "love.yinlin" to "love.yinlin.RachelMusicService"
    }

    // 数据仓库
    var playlist: MusicPlaylist? by mutableRefStateOf(null)
        private set
    val library = mutableStateMapOf<String, MusicInfo>()

    // 回调监听器
    val listener = object : MusicPlayerListener {
        override fun onMusicChanged(id: String?) {
            val lastPlaylist = playlist?.name ?: ""
            app.config.lastPlaylist = lastPlaylist
            if (lastPlaylist.isNotEmpty()) id?.let { app.config.lastMusic = it }
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
    val musicList get() = controller.musicList
    val currentId: String? get() = controller.currentId
    val currentMusic: MusicInfo? get() = controller.currentId?.let { library[it] }
    val error: Throwable? get() = controller.error
    suspend fun play() = controller.play()
    suspend fun pause() = controller.pause()
    suspend fun stop() = controller.stop()
    suspend fun gotoPrevious() = controller.gotoPrevious()
    suspend fun gotoNext() = controller.gotoNext()
    suspend fun gotoIndex(index: Int) = controller.gotoIndex(index)
    suspend fun seekTo(position: Long) = controller.seekTo(position)
    suspend fun addMedias(medias: List<String>) = controller.addMedias(medias)
    suspend fun removeMedia(index: Int) = controller.removeMedia(index)

    // 歌词引擎
    val engineHost = LyricsEngineHost { controller.seekTo(it) }
    var engine by mutableRefStateOf(LyricsEngine.Default)
    val floatingLyrics: FloatingLyrics = FloatingLyrics(this)

    private suspend fun initLibrary() {
        val items = Coroutines.io {
            app.modPath.list().mapNotNull {
                val configPath = File(app.modPath, it.name, ModResourceType.Config.filename)
                catchingNull { configPath.readText()!!.parseJsonValue<MusicInfo>() }
            }
        }
        for (item in items) library[item.id] = item
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
                val configPath = File(app.modPath, id, ModResourceType.Config.filename)
                val info = catchingNull { configPath.readText()!!.parseJsonValue<MusicInfo>() }
                if (info != null) library[id] = info.copy(modification = modification + 1)
            }
        }
    }

    suspend fun startPlaylist(playlist: MusicPlaylist, startId: String? = null, playing: Boolean) {
        if (controller.isInit) {
            if (this.playlist == playlist) {
                // 切换本歌单的其他歌曲
                if (currentId != startId && startId != null) {
                    val targetIndex = musicList.indexOf(startId)
                    if (targetIndex != -1) controller.gotoIndex(targetIndex)
                }
            }
            else {
                // 切换其他歌单
                val actualMusicList = playlist.items.filter { it in library }
                controller.stop()
                if (actualMusicList.isNotEmpty()) {
                    this.playlist = playlist
                    val index = if (startId != null) actualMusicList.indexOf(startId) else -1
                    controller.prepareMedias(actualMusicList, if (index != -1) index else null, playing)
                }
            }
        }
    }

    suspend fun switchPlayMode() {
        if (controller.isInit) controller.updatePlayMode(controller.playMode.next)
    }

    override suspend fun CoroutineScope.init(context: Context, args: StartupArgs) {
        launch {
            awaitAll(
                async { initLibrary() },
                async { controller.init(context) }
            )
            if (controller.isInit) {
                controller.listener = listener
                initLastStatus()
            }
        }
    }

    override suspend fun CoroutineScope.initLater(context: Context, args: StartupArgs) {
        launch { floatingLyrics.initDelay(context) }
    }

    override fun destroy(context: Context, args: StartupArgs) {
        controller.listener = null
        controller.release()
    }

    override val isSafeAccess: Boolean get() = isInit
}