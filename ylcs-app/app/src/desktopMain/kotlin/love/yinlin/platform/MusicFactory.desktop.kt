package love.yinlin.platform

import androidx.compose.runtime.*
import kotlinx.io.files.Path
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.extension.catching
import love.yinlin.extension.replaceAll
import love.yinlin.ui.screen.music.audioPath
import uk.co.caprica.vlcj.media.Media
import uk.co.caprica.vlcj.media.MediaEventAdapter
import uk.co.caprica.vlcj.media.MediaRef
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import kotlin.random.Random

class ActualMusicFactory : MusicFactory() {
    private var controller: AudioPlayerComponent? by mutableRefStateOf(null)
    override val isInit: Boolean by derivedStateOf { controller != null }

    override suspend fun init() = catching {
        val component = AudioPlayerComponent()
        component.mediaPlayer().events().apply {
            addMediaEventListener(eventListener)
            addMediaPlayerEventListener(playerListener)
        }
        controller = component
    }

    override var error: Throwable? by mutableRefStateOf(null)
    override var playMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
    override val musicList = mutableStateListOf<MusicInfo>()
    override val isReady: Boolean by derivedStateOf { musicList.isNotEmpty() }
    override var isPlaying: Boolean by mutableStateOf(false)
    override var currentPosition: Long by mutableLongStateOf(0L)
    override var currentDuration: Long by mutableLongStateOf(0L)
    override var currentMusic: MusicInfo? by mutableRefStateOf(null)

    private var currentIndex: Int by mutableIntStateOf(-1)

    class ShuffledOrder(size: Int = 0, start: Int? = null) {
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

    private var shuffledList = ShuffledOrder()

    private val eventListener = object : MediaEventAdapter() {
        override fun mediaDurationChanged(media: Media?, newDuration: Long) { currentDuration = newDuration }
    }

    private val playerListener = object : MediaPlayerEventAdapter() {
        override fun mediaChanged(mediaPlayer: MediaPlayer?, media: MediaRef?) {
            val musicInfo = musicList[currentIndex]
            if (musicInfo.id != currentMusic?.id) {
                currentMusic = musicInfo
                onMusicChanged(musicInfo)
            }
        }
        override fun playing(mediaPlayer: MediaPlayer?) { isPlaying = true }
        override fun paused(mediaPlayer: MediaPlayer?) { isPlaying = false }
        override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) { currentPosition = newTime }
        override fun stopped(mediaPlayer: MediaPlayer?) {
            // 可能是因为播放完当前媒体停止 也可能是手动停止(此处要排除)
            if (isReady && mediaPlayer != null) mediaPlayer.innerGotoIndex(when (playMode) {
                MusicPlayMode.ORDER -> loopNextIndex
                MusicPlayMode.LOOP -> currentIndex
                MusicPlayMode.RANDOM -> randomNextIndex ?: reshuffled()
            })
        }
        override fun error(mediaPlayer: MediaPlayer?) {
            mediaPlayer?.innerStop()
            error = IllegalStateException("MediaPlayerError")
        }
    }

    private fun MediaPlayer.innerStop() {
        musicList.clear()
        isPlaying = false
        currentPosition = 0L
        currentDuration = 0L
        currentMusic = null
        currentIndex = -1
        currentPlaylist = null
        shuffledList = ShuffledOrder()
        controls().stop()
        onPlayerStop()
    }

    private val loopPreviousIndex: Int get() = (currentIndex + musicList.size - 1) % musicList.size

    private val loopNextIndex: Int get() = (currentIndex + 1) % musicList.size

    private val randomPreviousIndex: Int? get() {
        val indices = shuffledList.indices
        val indexInShuffled = indices.indexOf(currentIndex)
        return if (indexInShuffled != -1 && indices.size == musicList.size) {
            val actualIndex = indices[(indexInShuffled + indices.size - 1) % indices.size]
            // 播放一轮结束, 重新洗牌
            if (actualIndex == shuffledList.begin) null else actualIndex
        }
        else -1
    }

    private val randomNextIndex: Int? get() {
        val indices = shuffledList.indices
        val indexInShuffled = indices.indexOf(currentIndex)
        return if (indexInShuffled != -1 && indices.size == musicList.size) {
            val actualIndex = indices[(indexInShuffled + 1) % indices.size]
            // 播放一轮结束, 重新洗牌
            if (actualIndex == shuffledList.begin) null else actualIndex
        } else -1
    }

    private fun reshuffled(size: Int? = null, start: Int? = null): Int {
        shuffledList = ShuffledOrder(size = size ?: shuffledList.indices.size, start = start)
        return shuffledList.begin
    }

    private fun MediaPlayer.innerGotoIndex(index: Int, playing: Boolean = true) {
        if (index in 0 ..< musicList.size) {
            currentIndex = index
            val uri = musicList[currentIndex].audioPath.toString()
            if (playing) media().play(uri)
            else media().prepare(uri)
        }
        else innerStop()
    }

    private inline fun withPlayer(block: (player: MediaPlayer) -> Unit) = controller?.mediaPlayer()?.let(block) ?: Unit
    private inline fun withReadyPlayer(block: (player: MediaPlayer) -> Unit) = withPlayer { if (isReady) block(it) }

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) {
        playMode = musicPlayMode
        onPlayModeChanged(musicPlayMode)
        // 重新换模式要重设洗牌顺序
        if (musicPlayMode == MusicPlayMode.RANDOM) reshuffled(start = currentIndex)
    }

    override suspend fun play() = withReadyPlayer { player ->
        if (!player.status().isPlaying) player.controls().play()
    }

    override suspend fun pause() = withReadyPlayer { player ->
        if (player.status().isPlaying) player.controls().pause()
    }

    override suspend fun stop() = withReadyPlayer { player -> player.innerStop() }

    override suspend fun gotoPrevious() = withReadyPlayer { player ->
        player.innerGotoIndex(if (playMode == MusicPlayMode.RANDOM) randomPreviousIndex ?: reshuffled() else loopPreviousIndex)
    }

    override suspend fun gotoNext() = withReadyPlayer { player ->
        player.innerGotoIndex(if (playMode == MusicPlayMode.RANDOM) randomNextIndex ?: reshuffled() else loopNextIndex)
    }

    override suspend fun gotoIndex(index: Int) = withReadyPlayer { player -> player.innerGotoIndex(index) }

    override suspend fun seekTo(position: Long) = withReadyPlayer { player ->
        player.controls().setTime(position)
        if (!player.status().isPlaying) player.controls().play()
    }

    override suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?, playing: Boolean) = withPlayer { player ->
        val index = startIndex ?: 0
        if (index >= 0 && index < medias.size) {
            musicList.replaceAll(medias)
            reshuffled(size = medias.size, start = index)
            player.innerGotoIndex(index, playing)
        }
    }

    override suspend fun addMedias(medias: List<MusicInfo>) = withReadyPlayer {
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

    override suspend fun removeMedia(index: Int) = withReadyPlayer { player ->
        val size = musicList.size
        if (index in 0 ..< size) {
            if (size == 1) player.innerStop()
            else {
                musicList.removeAt(index)
                if (playMode == MusicPlayMode.RANDOM) {
                    if (currentIndex == index) player.innerGotoIndex(reshuffled(size = size - 1))
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
                    if (currentIndex == index) player.innerGotoIndex(if (index == size - 1) index - 1 else index)
                    reshuffled(size = size - 1)
                }
            }
        }
    }
}

@Stable
actual class MusicPlayer {
    private external fun nativeCreatePlayer(): Long
    private external fun nativeReleasePlayer(handle: Long)
    private external fun nativeIsPlaying(handle: Long): Boolean
    private external fun nativeGetPosition(handle: Long): Long
    private external fun nativeGetDuration(handle: Long): Long
    private external fun nativeLoad(handle: Long, path: String)
    private external fun nativePlay(handle: Long)
    private external fun nativePause(handle: Long)
    private external fun nativeStop(handle: Long)

    private var handle: Long = 0L

    actual val isInit: Boolean get() = handle != 0L

    actual val isPlaying: Boolean get() = nativeIsPlaying(handle)

    actual val position: Long get() = nativeGetPosition(handle)

    actual val duration: Long get() = nativeGetDuration(handle)

    actual suspend fun init() {
        handle = nativeCreatePlayer()
    }

    actual suspend fun load(path: Path) {
        Coroutines.io {
            nativeLoad(handle, path.toString())
            nativePlay(handle)
        }
    }

    actual fun play() {
        nativePlay(handle)
    }

    actual fun pause() {
        nativePause(handle)
    }

    actual fun stop() {
        nativeStop(handle)
    }

    actual fun release() {
        nativeReleasePlayer(handle)
        handle = 0L
    }
}