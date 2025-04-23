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

    private var currentIndex: Int by mutableIntStateOf(-1)

    override var error: Throwable? by mutableStateOf(null)
    override var playMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
    override val musicList = mutableStateListOf<MusicInfo>()
    override val isReady: Boolean by derivedStateOf { musicList.isNotEmpty() }
    override var isPlaying: Boolean by mutableStateOf(false)
    override var currentPosition: Long by mutableLongStateOf(0L)
    override var currentDuration: Long by mutableLongStateOf(0L)
    override var currentMusic: MusicInfo? by mutableStateOf(null)

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
            if (isReady) mediaPlayer?.innerAutoGotoNext()
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

    private fun MediaPlayer.gotoLoopPrevious() {
        currentIndex = (currentIndex + musicList.size - 1) % musicList.size
        media().play(musicList[currentIndex].audioPath.toString())
    }

    private fun MediaPlayer.gotoLoopNext() {
        currentIndex = (currentIndex + 1) % musicList.size
        media().play(musicList[currentIndex].audioPath.toString())
    }

    private fun MediaPlayer.innerAutoGotoNext() {
        when (playMode) {
            MusicPlayMode.ORDER -> gotoLoopNext()
            MusicPlayMode.LOOP -> media().play(musicList[currentIndex].audioPath.toString())
            MusicPlayMode.RANDOM -> {

            }
        }
    }

    private inline fun withPlayer(block: (player: MediaPlayer) -> Unit) = controller?.mediaPlayer()?.let(block) ?: Unit
    private inline fun withReadyPlayer(block: (player: MediaPlayer) -> Unit) = withPlayer { if (isReady) block(it) }

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) {
        playMode = musicPlayMode
    }

    override suspend fun play() = withReadyPlayer { player ->
        if (!player.status().isPlaying) player.controls().play()
    }

    override suspend fun pause() = withReadyPlayer { player ->
        if (player.status().isPlaying) player.controls().pause()
    }

    override suspend fun stop() = withReadyPlayer { player ->
        player.innerStop()
    }

    override suspend fun gotoPrevious() = withReadyPlayer { player ->
        when (playMode) {
            MusicPlayMode.ORDER, MusicPlayMode.LOOP -> player.gotoLoopPrevious()
            MusicPlayMode.RANDOM -> {

            }
        }
    }

    override suspend fun gotoNext() = withReadyPlayer { player ->
        when (playMode) {
            MusicPlayMode.ORDER, MusicPlayMode.LOOP -> player.gotoLoopNext()
            MusicPlayMode.RANDOM -> {

            }
        }
    }

    override suspend fun gotoIndex(index: Int) = withReadyPlayer { player ->

    }

    override suspend fun seekTo(position: Long) = withReadyPlayer { player ->
        player.controls().setTime(position)
        if (!player.status().isPlaying) player.controls().play()
    }

    override suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?) = withPlayer { player ->
        val index = startIndex ?: 0
        if (index >= 0 && index < medias.size) {
            currentIndex = index
            musicList.replaceAll(medias)
            player.media().prepare(musicList[index].audioPath.toString())
        }
    }

    override suspend fun addMedia(media: MusicInfo) {

    }

    override suspend fun addMedias(medias: List<MusicInfo>) {

    }

    override suspend fun removeMedia(index: Int) {

    }

    override suspend fun moveMedia(start: Int, end: Int) {

    }
}