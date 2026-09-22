package love.yinlin.startup

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import love.yinlin.annotation.LooseTyped
import love.yinlin.app
import love.yinlin.common.SleepTimer
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.compose.ds.DataSourceMusic
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.ioContext
import love.yinlin.coroutines.mainContext
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.Playlist
import love.yinlin.extension.catchingError
import love.yinlin.extension.then
import love.yinlin.foundation.AsyncStartup
import love.yinlin.foundation.AsyncStartupFactory
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.media.MediaMetadataFetcher
import love.yinlin.media.MusicPlayerListener
import love.yinlin.media.buildMusicPlayer
import love.yinlin.media.lyrics.FloatingLyrics
import love.yinlin.media.lyrics.LyricsEngine
import love.yinlin.media.lyrics.LyricsEngineHost
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

@Stable
class StartupMusicPlayer(pool: StartupPool) : AsyncStartup(pool) {
    class Factory : AsyncStartupFactory<StartupMusicPlayer>() {
        override val id: String = StartupID<StartupMusicPlayer>()
        override val dependencies: List<String> = listOf(StartupID<StartupAppConfig>())
        override val dispatcher: CoroutineContext = mainContext
        override fun build(pool: StartupPool): StartupMusicPlayer = StartupMusicPlayer(pool)
    }

    // 外部数据提取器
    val fetcher = object : MediaMetadataFetcher {
        override val audioFocus: Boolean get() = app.config.audioFocus
        override val interval: Long get() = engine.interval

        override fun extractAudioUri(id: String): String? = DataSourceMusic.library[id]?.path(app.modPath, ModResourceType.Audio)?.path
        override fun extractCoverUri(id: String): String? = DataSourceMusic.library[id]?.path(app.modPath, ModResourceType.Record)?.path
        override fun extractMetadata(id: String): MediaInfo? = DataSourceMusic.library[id]

        @LooseTyped
        override val androidMusicServiceClassName: String = "love.yinlin.RachelMusicService"
    }

    private val scope = CoroutineScope(SupervisorJob() + mainContext)

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
    val currentMusic: MusicInfo? get() = controller.currentId?.let { DataSourceMusic.library[it] }
    val error: Throwable? get() = controller.error
    suspend fun play() = controller.play()
    suspend fun pause() = controller.pause()
    suspend fun stop() = controller.stop()
    suspend fun gotoPrevious() = controller.gotoPrevious()
    suspend fun gotoNext() = controller.gotoNext()
    suspend fun gotoIndex(index: Int) = controller.gotoIndex(index)
    suspend fun seekTo(position: Long) = controller.seekTo(position)
    suspend fun removeMedia(index: Int) = controller.removeMedia(index)
    suspend fun moveMedia(fromIndex: Int, toIndex: Int) = controller.moveMedia(fromIndex, toIndex)

    // 歌词引擎
    val engineHost = LyricsEngineHost { controller.seekTo(it) }
    var engine by mutableRefStateOf(LyricsEngine.Default)
        private set
    val floatingLyrics: FloatingLyrics = FloatingLyrics(this)

    // 显控属性
    var showCurrentTime: Long by mutableLongStateOf(0L)
        private set

    // 歌曲属性
    var currentHasAnimation by mutableStateOf(false)
        private set
    var currentHasVideo by mutableStateOf(false)
        private set
    var currentHasAccompaniment by mutableStateOf(false)
        private set
    var currentUseAnimationBackground by mutableRefStateOf(false)

    // 睡眠定时器
    val sleepTimer = SleepTimer(scope, ::stop)

    // 回调监听器
    val listener = object : MusicPlayerListener {
        override fun onPositionChanged(position: Long) {
            // 处理进度条
            if (abs(position - showCurrentTime) > 1000L - engine.interval) showCurrentTime = position
            // 处理歌词
            engine.update(position)
        }

        override fun onMusicChanged(id: String?) {
            // 更新配置
            val config = app.config
            val lastPlaylist = DataSourceMusic.playlist
            if (config.lastPlaylist.name != lastPlaylist.name) config.lastPlaylist = lastPlaylist
            config.lastMusic = if (lastPlaylist is Playlist.None) "" else id ?: ""

            // 重置引擎
            engine.reset()

            val modPath = app.modPath
            val music = if (id != null) DataSourceMusic.library[id] else null
            if (music != null) {
                scope.launch(ioContext) {
                    Coroutines.catchingNull {
                        // 按引擎顺序依次检查是否成功加载
                        val rootPath = music.path(modPath)
                        var currentEngine = engine
                        for (engineType in app.config.lyricsEngineOrder) {
                            val newEngine = LyricsEngine[engineType]
                            if (newEngine.load(rootPath)) {
                                currentEngine = newEngine
                                break
                            }
                        }
                        // 更新引擎
                        engine = currentEngine
                        // 更新状态标志
                        currentHasAnimation = music.path(modPath, ModResourceType.Animation).isFile()
                        currentHasVideo = music.path(modPath,ModResourceType.Video).isFile()
                        currentHasAccompaniment = music.path(modPath,ModResourceType.Accompaniment).isFile()
                    }
                }
            }
            else {
                // 重置状态
                currentHasAnimation = false
                currentHasVideo = false
                currentHasAccompaniment = false
                // 结束睡眠模式
                sleepTimer.stop()
            }

            // 更新动画状态
            if (currentUseAnimationBackground && !currentHasAnimation) currentUseAnimationBackground = false
        }

        override fun onPlayModeChanged(mode: MediaPlayMode) {
            app.config.musicPlayMode = mode
        }

        override fun onPlayerStop() {
            DataSourceMusic.updatePlaylist(Playlist.None)
            app.config.lastPlaylist = Playlist.None
            app.config.lastMusic = ""
        }
    }

    /**
     * 载入上一次播放状态
     */
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

    /**
     * 重载歌单
     *
     * 因为可能存在已经被删除的媒体, 但歌单中仍然保留。
     * 若此时歌曲被导入则当前歌单需要更新当前列表
     */
    suspend fun reloadPlaylist(ids: Iterable<String>) {
        val actualMusicList = DataSourceMusic.fetchCurrentPlaylist(DataSourceMusic.playlist)
        if (actualMusicList.isNotEmpty() && ids.any { it in actualMusicList }) {
            controller.resetMedias(actualMusicList)
        }
    }

    enum class ReloadAddData {
        None, // 空列表
        Playing, // 正在播放
        Replace, // 替换原 Item
        Restore, // 存储新 Item
        DefaultPlaylist, // 默认歌单
    }

    /**
     * 检查是否需要因添加而重载歌单
     */
    fun checkReloadPlaylistByAdd(id: String): ReloadAddData {
        if (DataSourceMusic.playlist == Playlist.Default) return ReloadAddData.DefaultPlaylist
        val actualMusicList = DataSourceMusic.fetchCurrentPlaylist(DataSourceMusic.playlist)
        val rawIndex = actualMusicList.indexOf(id)
        if (actualMusicList.isNotEmpty() && rawIndex != -1) {
            val index = musicList.indexOf(id)
            return if (index != -1) { // 在当前播放列表
                // 是当前播放的歌曲立即阻止更新, 否则替换对应 Item
                if (id == currentId) ReloadAddData.Playing else ReloadAddData.Replace
            } else ReloadAddData.Restore
        }
        return ReloadAddData.None // 无事发生
    }

    /**
     * 因添加而重载歌单
     */
    suspend fun reloadPlaylistByAdd(id: String, data: ReloadAddData) {
        // 重新获取防止默认歌单未更新
        val actualMusicList = DataSourceMusic.fetchCurrentPlaylist(DataSourceMusic.playlist)
        val rawIndex = actualMusicList.indexOf(id)
        when (data) {
            ReloadAddData.None, ReloadAddData.Playing -> { }
            ReloadAddData.Replace -> {
                val index = musicList.indexOf(id)
                if (index != -1) controller.replaceMedia(index)
            }
            ReloadAddData.DefaultPlaylist, ReloadAddData.Restore -> { // 加入默认歌单 or 恢复已删除的媒体到歌单
                var insertIndex: Int? = null
                // 遍历原始歌单找到待恢复歌曲往后最先遇到的且在播放列表里的歌曲
                for (i in rawIndex + 1 ..< actualMusicList.size) {
                    val targetIndex = musicList.indexOf(actualMusicList[i])
                    if (targetIndex != -1) {
                        insertIndex = targetIndex
                        break
                    }
                }
                // 将待恢复歌曲插入到此位置上
                if (insertIndex != null) controller.addMedia(id, insertIndex)
                else controller.addMedia(id)
            }
        }
    }

    /**
     * 检查是否需要因删除而重载歌单
     *
     * 返回值是当前歌单中包含待删除的ID组
     */
    fun checkReloadPlaylistByDelete(ids: List<String>): List<String>? {
        val currentMusicList = musicList
        if (currentMusicList.isEmpty()) return emptyList()
        val useList = mutableListOf<String>()
        for (id in ids) {
            when (id) {
                currentId -> return null // 正在播放, 立即阻止更新
                in currentMusicList -> useList += id // 在当前播放列表里
                else -> { } // 不在播放列表里
            }
        }
        return useList
    }

    suspend fun reloadPlaylistByDelete(useList: List<String>) {
        if (useList.isEmpty()) return
        controller.removeMedias(useList)
    }

    suspend fun startPlaylist(newPlaylist: Playlist, startId: String? = null, playing: Boolean) {
        if (!controller.isInit || newPlaylist is Playlist.None) return
        if (DataSourceMusic.playlist == newPlaylist) {
            // 切换本歌单的其他歌曲
            if (currentId != startId && startId != null) {
                val targetIndex = musicList.indexOf(startId)
                if (targetIndex != -1) controller.gotoIndex(targetIndex)
            }
        }
        else {
            // 切换其他歌单
            val actualMusicList = DataSourceMusic.fetchCurrentPlaylist(newPlaylist)
            if (actualMusicList.isNotEmpty()) {
                controller.stop()
                DataSourceMusic.updatePlaylist(newPlaylist)
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
                // 初始化曲库
                async { DataSourceMusic.initLibrary() },
                // 初始化播放器
                async { controller.init(pool.rawContext) }
            )
            if (controller.isInit) {
                // 设置监听器
                controller.listener = listener
                // 载入上次播放状态
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
        scope.cancel()
    }
}