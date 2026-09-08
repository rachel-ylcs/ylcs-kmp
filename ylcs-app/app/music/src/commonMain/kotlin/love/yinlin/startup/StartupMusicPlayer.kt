package love.yinlin.startup

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastFilter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import love.yinlin.annotation.LooseTyped
import love.yinlin.app
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.mainContext
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.Playlist
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.then
import love.yinlin.foundation.AsyncStartup
import love.yinlin.foundation.AsyncStartupFactory
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.fs.File
import love.yinlin.media.MediaMetadataFetcher
import love.yinlin.media.MusicPlayerListener
import love.yinlin.media.buildMusicPlayer
import love.yinlin.media.lyrics.FloatingLyrics
import love.yinlin.media.lyrics.LyricsEngine
import love.yinlin.media.lyrics.LyricsEngineHost
import kotlin.coroutines.CoroutineContext

@Stable
class StartupMusicPlayer(pool: StartupPool) : AsyncStartup(pool) {
    class Factory : AsyncStartupFactory<StartupMusicPlayer>() {
        override val id: String = StartupID<StartupMusicPlayer>()
        override val dependencies: List<String> = listOf(StartupID<StartupAppConfig>())
        override val dispatcher: CoroutineContext = mainContext
        override fun build(pool: StartupPool): StartupMusicPlayer = StartupMusicPlayer(pool)
    }

    private fun MusicInfo.path(type: ModResourceType) = this.path(app.modPath, type)

    // 外部数据提取器
    val fetcher = object : MediaMetadataFetcher {
        override val audioFocus: Boolean get() = app.config.audioFocus
        override val interval: Long get() = engine.interval

        override fun extractAudioUri(id: String): String? = library[id]?.path(ModResourceType.Audio)?.path
        override fun extractCoverUri(id: String): String? = library[id]?.path(ModResourceType.Record)?.path
        override fun extractMetadata(id: String): MediaInfo? = library[id]

        @LooseTyped
        override val androidMusicServiceClassName: String = "love.yinlin.RachelMusicService"
    }

    // 数据仓库
    var playlist: Playlist by mutableStateOf(Playlist.None)
        private set
    val library = mutableStateMapOf<String, MusicInfo>()

    // 回调监听器
    val listener = object : MusicPlayerListener {
        override fun onMusicChanged(id: String?) {
            val lastPlaylist = playlist
            app.config.lastPlaylist = lastPlaylist
            app.config.lastMusic = if (lastPlaylist is Playlist.None) "" else id ?: ""
        }

        override fun onPlayModeChanged(mode: MediaPlayMode) {
            app.config.musicPlayMode = mode
        }

        override fun onPlayerStop() {
            playlist = Playlist.None
            app.config.lastPlaylist = Playlist.None
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
    val musicList: List<String> get() = controller.musicList
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
    suspend fun updateNewMedias(medias: List<String>) = controller.updateNewMedias(medias)
    suspend fun removeMedia(index: Int) = controller.removeMedia(index)
    suspend fun moveMedia(fromIndex: Int, toIndex: Int) = controller.moveMedia(fromIndex, toIndex)

    // 歌词引擎
    val engineHost = LyricsEngineHost { controller.seekTo(it) }
    var engine by mutableRefStateOf(LyricsEngine.Default)
    val floatingLyrics: FloatingLyrics = FloatingLyrics(this)

    private suspend fun initLibrary() {
        val items = Coroutines.io {
            app.modPath.list().mapNotNull {
                val configPath = File(app.modPath, it.name, ModResourceType.Config.filename)
                try {
                    configPath.readText()!!.parseJsonValue<MusicInfo>()
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
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
        }?.then { engine = LyricsEngine.Default }
        // 恢复上一次播放
        startPlaylist(app.config.lastPlaylist, app.config.lastMusic.ifEmpty { null }, false)
    }

    // 检查歌曲是否在当前播放列表中
    fun checkMusicIsInCurrentPlaylist(items: List<String>): String? = items.firstOrNull { it in musicList }

    private fun fetchCurrentPlaylist(list: Playlist): List<String> = when (list) {
        is Playlist.None -> emptyList()
        is Playlist.Default -> library.values.map { it.id }
        is Playlist.User -> app.config.playlistLibrary[list.name]?.items?.fastFilter { it in library } ?: emptyList()
    }

    suspend fun updateMusicLibraryInfo(ids: List<String>) {
        // 更新曲库
        val newInfoList = Coroutines.io {
            buildMap {
                for (id in ids) {
                    val modification = library[id]?.modification ?: 0
                    val configPath = File(app.modPath, id, ModResourceType.Config.filename)
                    val info = catchingNull { configPath.readText()!!.parseJsonValue<MusicInfo>() }
                    if (info != null) put(id, info.copy(modification = modification + 1))
                }
            }
        }
        library.putAll(newInfoList)
        // 更新当前播放列表
        val actualMusicList = fetchCurrentPlaylist(playlist)
        if (actualMusicList.isNotEmpty()) updateNewMedias(actualMusicList)
    }

    suspend fun startPlaylist(newPlaylist: Playlist, startId: String? = null, playing: Boolean) {
        if (!controller.isInit || newPlaylist is Playlist.None) return
        if (playlist == newPlaylist) {
            // 切换本歌单的其他歌曲
            if (currentId != startId && startId != null) {
                val targetIndex = musicList.indexOf(startId)
                if (targetIndex != -1) controller.gotoIndex(targetIndex)
            }
        }
        else {
            // 切换其他歌单
            val actualMusicList = fetchCurrentPlaylist(newPlaylist)
            if (actualMusicList.isNotEmpty()) {
                controller.stop()
                playlist = newPlaylist
                val index = if (startId != null) actualMusicList.indexOf(startId) else -1
                controller.prepareMedias(actualMusicList, if (index != -1) index else null, playing)
            }
        }
    }

    suspend fun switchPlayMode() {
        if (controller.isInit) controller.updatePlayMode(controller.playMode.next)
    }

    override suspend fun init() {
        coroutineScope {
            awaitAll(
                async { initLibrary() },
                async { controller.init(pool.rawContext) }
            )
            if (controller.isInit) {
                controller.listener = listener
                initLastStatus()
            }
        }
    }

    override suspend fun initLater() {
        floatingLyrics.initDelay()
    }

    override fun destroy() {
        controller.listener = null
        controller.release()
    }
}