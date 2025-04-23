@file:JvmName("MusicFactoryDesktop")
package love.yinlin.platform

import androidx.compose.runtime.*
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.extension.replaceAll
import love.yinlin.ui.screen.music.audioPath
import uk.co.caprica.vlcj.media.Media
import uk.co.caprica.vlcj.media.MediaEventAdapter
import uk.co.caprica.vlcj.media.MediaRef
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent

class ActualMusicFactory : MusicFactory() {
    private var controller: AudioPlayerComponent? by mutableStateOf(null)
    override val isInit: Boolean by derivedStateOf { controller != null }

    override suspend fun init() {
        try {
            System.setProperty("jna.library.path", "vlc")
            val component = AudioPlayerComponent()
            component.mediaPlayer().events().apply {
                addMediaEventListener(eventListener)
                addMediaPlayerEventListener(playerListener)
            }
            controller = component
        }
        catch (_: Throwable) { }
    }

    override var error: Throwable? by mutableStateOf(null)
    override var playMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
    override val musicList = mutableStateListOf<MusicInfo>()
    override val isReady: Boolean by derivedStateOf { musicList.isNotEmpty() }
    override var isPlaying: Boolean by mutableStateOf(false)
    override var currentPosition: Long by mutableLongStateOf(0L)
    override var currentDuration: Long by mutableLongStateOf(0L)
    override var currentMusic: MusicInfo? by mutableStateOf(null)

    private var currentIndex: Int by mutableIntStateOf(-1)

    class ShuffledOrder(size: Int = 0, start: Int? = null) {
        val indices: List<Int> = List(size) { it }.shuffled()
        val begin: Int = start ?: indices.firstOrNull() ?: -1

        override fun toString(): String = "ShuffledOrder($begin) [${indices.joinToString(",")}]"
    }

    private var shuffledList = ShuffledOrder()

    private val eventListener = object : MediaEventAdapter() {
        override fun mediaDurationChanged(media: Media?, newDuration: Long) { currentDuration = newDuration }
    }

    private val playerListener = object : MediaPlayerEventAdapter() {
        override fun mediaChanged(mediaPlayer: MediaPlayer?, media: MediaRef?) { currentMusic = musicList[currentIndex] }
        override fun playing(mediaPlayer: MediaPlayer?) { isPlaying = true }
        override fun paused(mediaPlayer: MediaPlayer?) { isPlaying = false }
        override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) { currentPosition = newTime }
        override fun stopped(mediaPlayer: MediaPlayer?) {
            // 可能是因为播放完当前媒体停止 也可能是手动停止(此处要排除)
            if (isReady && mediaPlayer != null) mediaPlayer.innerGotoIndex(innerAutoNextIndex)
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
        currentPlaylist = null
        controls().stop()
    }

    private val loopPreviousIndex: Int get() = (currentIndex + musicList.size - 1) % musicList.size

    private val loopNextIndex: Int get() = (currentIndex + 1) % musicList.size

    private val randomPreviousIndex: Int get() {
        val indices = shuffledList.indices
        val indexInShuffled = indices.indexOf(currentIndex)
        return if (indexInShuffled != -1 && indices.size == musicList.size) {
            val actualIndex = indices[(indexInShuffled + indices.size - 1) % indices.size]
            // 播放一轮结束, 重新洗牌
            if (actualIndex == shuffledList.begin) {
                shuffledList = ShuffledOrder(size = indices.size)
                shuffledList.begin
            }
            else actualIndex
        }
        else -1
    }

    private val randomNextIndex: Int get() {
        val indices = shuffledList.indices
        val indexInShuffled = indices.indexOf(currentIndex)
        return if (indexInShuffled != -1 && indices.size == musicList.size) {
            val actualIndex = indices[(indexInShuffled + 1) % indices.size]
            // 播放一轮结束, 重新洗牌
            if (actualIndex == shuffledList.begin) {
                shuffledList = ShuffledOrder(size = indices.size)
                shuffledList.begin
            }
            else actualIndex
        } else -1
    }

    private val innerAutoNextIndex: Int get() = when (playMode) {
        MusicPlayMode.ORDER -> loopNextIndex
        MusicPlayMode.LOOP -> currentIndex
        MusicPlayMode.RANDOM -> randomNextIndex
    }

    private fun MediaPlayer.innerGotoIndex(index: Int) {
        if (index in 0 ..< musicList.size) {
            currentIndex = index
            media().play(musicList[currentIndex].audioPath.toString())
        }
        else innerStop()
    }

    private inline fun withPlayer(block: (player: MediaPlayer) -> Unit) = controller?.mediaPlayer()?.let(block) ?: Unit
    private inline fun withReadyPlayer(block: (player: MediaPlayer) -> Unit) = withPlayer { if (isReady) block(it) }

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) {
        playMode = musicPlayMode
        // 重新换模式要重设洗牌顺序
        if (musicPlayMode == MusicPlayMode.RANDOM) {
            shuffledList = ShuffledOrder(size = musicList.size, start = currentIndex)
        }
    }

    override suspend fun play() = withReadyPlayer { player ->
        if (!player.status().isPlaying) player.controls().play()
    }

    override suspend fun pause() = withReadyPlayer { player ->
        if (player.status().isPlaying) player.controls().pause()
    }

    override suspend fun stop() = withReadyPlayer { player -> player.innerStop() }

    override suspend fun gotoPrevious() = withReadyPlayer { player ->
        player.innerGotoIndex(if (playMode == MusicPlayMode.RANDOM) randomPreviousIndex else loopPreviousIndex)
    }

    override suspend fun gotoNext() = withReadyPlayer { player ->
        player.innerGotoIndex(if (playMode == MusicPlayMode.RANDOM) randomNextIndex else loopNextIndex)
    }

    override suspend fun gotoIndex(index: Int) = withReadyPlayer { player -> player.innerGotoIndex(index) }

    override suspend fun seekTo(position: Long) = withReadyPlayer { player ->
        player.controls().setTime(position)
        if (!player.status().isPlaying) player.controls().play()
    }

    override suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?) = withPlayer { player ->
        val index = startIndex ?: 0
        if (index >= 0 && index < medias.size) {
            musicList.replaceAll(medias)
            shuffledList = ShuffledOrder(size = medias.size, start = index)
            player.innerGotoIndex(index)
        }
    }

    override suspend fun addMedias(medias: List<MusicInfo>) {
        
    }

    override suspend fun removeMedia(index: Int) {

    }
}