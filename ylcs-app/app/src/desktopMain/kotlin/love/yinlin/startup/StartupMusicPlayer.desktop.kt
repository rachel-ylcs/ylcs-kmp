package love.yinlin.startup

import androidx.compose.runtime.*
import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.StartupFetcher
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.extension.replaceAll
import love.yinlin.platform.Coroutines
import love.yinlin.platform.Platform
import love.yinlin.platform.WindowsNativeAudioPlayer
import love.yinlin.platform.WindowsNativePlaybackState
import love.yinlin.platform.platform
import kotlin.random.Random

private class ShuffledOrder(size: Int = 0, start: Int? = null) {
    var indices: List<Int> = List(size) { it }.shuffled()
        private set
    var begin: Int = start ?: indices.firstOrNull() ?: -1
        private set

    fun internalSet(items: List<Int>, start: Int? = null) {
        indices = items
        if (start != null) begin = start
    }

    override fun toString(): String = "ShuffledOrder($begin) [${indices.joinToString(",")}]"
}

@Stable
private abstract class PlatformMusicPlayer : StartupMusicPlayer() {
    protected var currentIndex: Int by mutableIntStateOf(-1)
    protected var shuffledList = ShuffledOrder()

    override var error: Throwable? by mutableRefStateOf(null)
    override var playMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
    override val musicList = mutableStateListOf<MusicInfo>()
    override val isReady: Boolean by derivedStateOf { musicList.isNotEmpty() }
    override var isPlaying: Boolean by mutableStateOf(false)
    override var currentDuration: Long by mutableLongStateOf(0L)
    override var currentPosition: Long by mutableLongStateOf(0L)
    override var currentMusic: MusicInfo? by mutableRefStateOf(null)

    abstract fun innerStop()
    abstract fun innerGotoIndex(index: Int, playing: Boolean = true)

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) {
        playMode = musicPlayMode
        onPlayModeChanged(musicPlayMode)
        // 重新换模式要重设洗牌顺序
        if (musicPlayMode == MusicPlayMode.RANDOM) reshuffled(start = currentIndex)
    }

    protected val loopPreviousIndex: Int get() = (currentIndex + musicList.size - 1) % musicList.size

    protected val loopNextIndex: Int get() = (currentIndex + 1) % musicList.size

    protected val randomPreviousIndex: Int? get() {
        val indices = shuffledList.indices
        val indexInShuffled = indices.indexOf(currentIndex)
        return if (indexInShuffled != -1 && indices.size == musicList.size) {
            val actualIndex = indices[(indexInShuffled + indices.size - 1) % indices.size]
            // 播放一轮结束, 重新洗牌
            if (actualIndex == shuffledList.begin) null else actualIndex
        }
        else -1
    }

    protected val randomNextIndex: Int? get() {
        val indices = shuffledList.indices
        val indexInShuffled = indices.indexOf(currentIndex)
        return if (indexInShuffled != -1 && indices.size == musicList.size) {
            val actualIndex = indices[(indexInShuffled + 1) % indices.size]
            // 播放一轮结束, 重新洗牌
            if (actualIndex == shuffledList.begin) null else actualIndex
        } else -1
    }

    protected fun reshuffled(size: Int? = null, start: Int? = null): Int {
        shuffledList = ShuffledOrder(size = size ?: shuffledList.indices.size, start = start)
        return shuffledList.begin
    }

    override suspend fun stop() { if (isReady) innerStop() }

    override suspend fun gotoPrevious() {
        if (isReady) innerGotoIndex(if (playMode == MusicPlayMode.RANDOM) randomPreviousIndex ?: reshuffled() else loopPreviousIndex)
    }

    override suspend fun gotoNext() {
        if (isReady) innerGotoIndex(if (playMode == MusicPlayMode.RANDOM) randomNextIndex ?: reshuffled() else loopNextIndex)
    }

    override suspend fun gotoIndex(index: Int) {
        if (isReady) innerGotoIndex(index)
    }

    override suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?, playing: Boolean) {
        val index = startIndex ?: 0
        if (index >= 0 && index < medias.size) {
            musicList.replaceAll(medias)
            reshuffled(size = medias.size, start = index)
            innerGotoIndex(index, playing)
        }
    }

    override suspend fun addMedias(medias: List<MusicInfo>) {
        if (isReady) {
            musicList.addAll(medias)

            /*
            * 将当前播放位置随机序前面的索引接到随机序末尾
            * 保持第一个元素是始终是当前播放索引
            * 然后将新导入的媒体随机插入其中
            * 得到新的随机序索引表
            *  */
            val indices = shuffledList.indices
            if (playMode == MusicPlayMode.RANDOM) {
                val indexInShuffled = indices.indexOf(currentIndex)
                val rotated = indices.subList(indexInShuffled, indices.size) + indices.subList(0, indexInShuffled)
                val head = rotated.first()
                val rest = rotated.drop(1).toMutableList()
                repeat(medias.size) {
                    // 生成0到当前rest长度的随机位置
                    val position = Random.nextInt(rest.size + 1)
                    rest.add(position, indices.size + it)
                }
                shuffledList.internalSet(listOf(head) + rest)
            }
            else reshuffled(size = indices.size + medias.size)
        }
    }

    override suspend fun removeMedia(index: Int) {
        if (isReady) {
            val size = musicList.size
            if (index in musicList.indices) {
                if (size == 1) innerStop()
                else {
                    musicList.removeAt(index)
                    if (playMode == MusicPlayMode.RANDOM) {
                        if (currentIndex == index) innerGotoIndex(reshuffled(size = size - 1))
                        else {
                            // 如果删除了随机序的起点则重新令当前播放的为起点
                            val rest = shuffledList.indices.toMutableList()
                            rest.removeIf { it == index }
                            // 随机序中超过被删除索引的索引都要自减1
                            for ((i, item) in rest.withIndex()) {
                                if (item > index) rest[i] -= 1
                            }
                            shuffledList.internalSet(rest, if (shuffledList.begin == index) currentIndex else null)
                        }
                    }
                    else {
                        if (currentIndex == index) innerGotoIndex(if (index == size - 1) index - 1 else index)
                        reshuffled(size = size - 1)
                    }
                }
            }
        }
    }
}

@StartupFetcher(index = 0, name = "rootPath", returnType = Path::class, nullable = true)
private class WindowsMusicPlayer : PlatformMusicPlayer() {
    private val controller = WindowsNativeAudioPlayer()
    private var shouldImmediatePlay: Boolean = false

    override var isInit: Boolean by mutableStateOf(false)
        private set

    private val listener = object : WindowsNativeAudioPlayer.Listener() {
        override fun onPositionChange(position: Long) {
            currentPosition = position
        }

        override fun onDurationChange(duration: Long) {
            currentDuration = duration
        }

        override fun onPlaybackStateChange(state: WindowsNativePlaybackState) {
            when (state) {
                WindowsNativePlaybackState.Playing -> isPlaying = true
                WindowsNativePlaybackState.Paused, WindowsNativePlaybackState.None -> isPlaying = false
                WindowsNativePlaybackState.Opening -> {
                    if (shouldImmediatePlay) controller.play()
                }
                else -> {}
            }
        }

        override fun onSourceChange() {
            if (isReady && currentIndex != -1) {
                val musicInfo = musicList[currentIndex]
                if (musicInfo.id != currentMusic?.id) {
                    currentMusic = musicInfo
                    onMusicChanged(musicInfo)
                }
            }
        }

        override fun onMediaEnded() {
            if (isReady) innerGotoIndex(when (playMode) {
                MusicPlayMode.ORDER -> loopNextIndex
                MusicPlayMode.LOOP -> currentIndex
                MusicPlayMode.RANDOM -> randomNextIndex ?: reshuffled()
            })
        }
    }

    override suspend fun initController(context: Context) {
        isInit = Coroutines.io { controller.create(listener) }
    }

    override suspend fun play() {
        if (isReady && controller.playbackState != WindowsNativePlaybackState.Playing) controller.play()
    }

    override suspend fun pause() {
        if (isReady && controller.playbackState == WindowsNativePlaybackState.Playing) controller.pause()
    }

    override suspend fun seekTo(position: Long) {
        if (isReady) {
            controller.seek(position)
            if (controller.playbackState != WindowsNativePlaybackState.Playing) controller.play()
        }
    }

    override fun innerStop() {
        musicList.clear()
        currentMusic = null
        currentDuration = 0L
        currentIndex = -1
        playlist = null
        shuffledList = ShuffledOrder()
        controller.stop()
        onPlayerStop()
    }

    override fun innerGotoIndex(index: Int, playing: Boolean) {
        if (index in musicList.indices) {
            currentIndex = index
            val uri = musicList[currentIndex].path(ModResourceType.Audio).toString()
            controller.load(uri)
            shouldImmediatePlay = playing
        }
        else innerStop()
    }
}

@StartupFetcher(index = 0, name = "rootPath", returnType = Path::class, nullable = true)
@Stable
actual fun buildMusicPlayer(): StartupMusicPlayer = when (platform) {
    Platform.Linux -> WindowsMusicPlayer()
    Platform.MacOS -> WindowsMusicPlayer()
    else -> WindowsMusicPlayer()
}