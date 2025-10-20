package love.yinlin.platform

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cocoapods.MobileVLCKit.*
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.io.files.Path
import love.yinlin.compose.mutableRefStateOf
import kotlin.math.roundToLong
import love.yinlin.ui.screen.music.audioPath
import love.yinlin.ui.screen.music.recordPath
import platform.darwin.*
import platform.Foundation.*
import platform.AVFAudio.*
import platform.MediaPlayer.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
class ActualMusicFactory : MusicFactory() {
    private var interruptionObserver: NSObjectProtocol? = null

    private var mediaPlayer: VLCMediaPlayer? = null
    private var playerDelegate: VLCMediaPlayerDelegateProtocol? = null
    private var shuffleList = listOf<Int>()
    private var currentIndex = -1

    override val isInit: Boolean get() = mediaPlayer != null
    override var error: Throwable? by mutableRefStateOf(null)
    override var playMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
    override var musicList: List<MusicInfo> by mutableRefStateOf(emptyList())
    override val isReady: Boolean by derivedStateOf { musicList.isNotEmpty() }
    override var isPlaying: Boolean by mutableStateOf(false)
    override var currentPosition: Long by mutableLongStateOf(0L)
    override var currentDuration: Long by mutableLongStateOf(0L)
    override var currentMusic: MusicInfo? by mutableRefStateOf(null)

    enum class AudioSessionInterruption {
        Began, Ended, Failed;
    }

    override suspend fun init() {
        if (isInit) return

        AVAudioSession.sharedInstance().apply {
            try {
                interruptionObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                    AVAudioSessionInterruptionNotification, this, NSOperationQueue.mainQueue
                ) { notification ->
                    notification?.userInfo?.let { userInfo ->
                        val interruptionType = userInfo[AVAudioSessionInterruptionTypeKey] as Long
                        when (interruptionType.toULong()) {
                            AVAudioSessionInterruptionTypeBegan -> {
                                handleAudioSessionInterruption(AudioSessionInterruption.Began, null)
                            }
                            AVAudioSessionInterruptionTypeEnded -> {
                                val options = userInfo[AVAudioSessionInterruptionOptionKey] as Long
                                val shouldResume = (options.toULong() and AVAudioSessionInterruptionOptionShouldResume) != 0UL
                                handleAudioSessionInterruption(AudioSessionInterruption.Ended, shouldResume)
                            }
                            else -> {
                                handleAudioSessionInterruption(AudioSessionInterruption.Failed, null)
                            }
                        }
                    }
                }
                val options = if (app.config.audioFocus) 0UL else AVAudioSessionCategoryOptionMixWithOthers
                setCategory(AVAudioSessionCategoryPlayback, options, null)
                setActive(true, null)
                setupNowPlayingInfoCenter()
            } catch (e: Throwable) {
                error = e
            }
        }

        playerDelegate = object : NSObject(), VLCMediaPlayerDelegateProtocol {
            override fun mediaPlayerStateChanged(aNotification: NSNotification) {
                mediaPlayer?.let { player ->
                    if (player.state == VLCMediaPlayerState.VLCMediaPlayerStateEnded) {
                        Coroutines.startMain {
                            when (playMode) {
                                MusicPlayMode.LOOP -> setCurrentPlaying(currentIndex, true)
                                else -> gotoNext()
                            }
                        }
                    } else {
                        isPlaying = player.playing
                        if (player.state == VLCMediaPlayerState.VLCMediaPlayerStateOpening) {
                            currentMusic = musicList[shuffleList[currentIndex]]
                            onMusicChanged(currentMusic)
                        }
                        if (player.state == VLCMediaPlayerState.VLCMediaPlayerStatePlaying) {
                            currentDuration = player.media?.length?.intValue?.toLong() ?: 0
                        }
                        updatePlayingMetadata()
                    }
                }
            }

            override fun mediaPlayerTimeChanged(aNotification: NSNotification) {
                mediaPlayer?.let { player ->
                    currentPosition = player.time.intValue.toLong()
                    updatePlayingInfo()
                }
            }
        }

        mediaPlayer = VLCMediaPlayer()
        mediaPlayer!!.delegate = playerDelegate
    }

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) {
        playMode = musicPlayMode
        if (musicList.isNotEmpty() && currentIndex > -1) {
            shufflePlayList()
        }
        onPlayModeChanged(musicPlayMode)
    }

    override suspend fun play(): Unit = Coroutines.main {
        mediaPlayer?.play()
    }

    override suspend fun pause(): Unit = Coroutines.main {
        mediaPlayer?.pause()
    }

    override suspend fun stop(): Unit = Coroutines.main {
        musicList = emptyList()
        currentPlaylist = null
        currentMusic = null
        currentIndex = -1
        shuffleList = listOf()
        mediaPlayer?.stop()
        mediaPlayer?.media = null
        currentPosition = 0L
        currentDuration = 0L
        onPlayerStop()
    }

    override suspend fun gotoPrevious() {
        if (musicList.isNotEmpty() && currentIndex == 0) {
            shufflePlayList()
        }
        setCurrentPlaying((currentIndex + shuffleList.size - 1) % shuffleList.size, true)
    }

    override suspend fun gotoNext() {
        if (musicList.isNotEmpty() && currentIndex == shuffleList.size - 1) {
            shufflePlayList()
        }
        setCurrentPlaying((currentIndex + 1) % shuffleList.size, true)
    }

    override suspend fun gotoIndex(index: Int) {
        if (index in 0 ..< shuffleList.size) {
            setCurrentPlaying(shuffleList.indexOf(index), true)
        }
    }

    override suspend fun seekTo(position: Long): Unit = Coroutines.main {
        mediaPlayer?.time = VLCTime.timeWithInt(position.toInt())
    }

    override suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?, playing: Boolean) {
        musicList = medias

        if (medias.isNotEmpty()) {
            val index = startIndex?.coerceIn(0, musicList.size - 1) ?: 0
            shufflePlayList(index)
            setCurrentPlaying(currentIndex, playing)
        }
    }

    override suspend fun addMedias(medias: List<MusicInfo>) {
        if (medias.isEmpty()) return

        val oldSize = musicList.size
        musicList = musicList + medias

        if (oldSize == 0) {
            shufflePlayList()
            if (musicList.isNotEmpty()) {
                setCurrentPlaying(currentIndex, false)
            }
        } else {
            shuffleList += (oldSize until musicList.size).toList().let {
                if (playMode == MusicPlayMode.RANDOM) it.shuffled() else it
            }
        }
    }

    override suspend fun removeMedia(index: Int) {
        if (index !in 0 ..< musicList.size) return

        Coroutines.main {
            val shuffleIndex = shuffleList.indexOf(index)
            val wasCurrent = shuffleIndex == currentIndex
            musicList = musicList.filterIndexed { id, _ -> id != index }

            when {
                musicList.isEmpty() -> {
                    stop()
                    return@main
                }
                wasCurrent -> {
                    if (currentIndex >= musicList.size) {
                        currentIndex = 0
                    }
                }
                shuffleIndex < currentIndex -> {
                    currentIndex--
                }
            }

            val newShuffleList = shuffleList.toMutableList()
            newShuffleList.removeAt(shuffleIndex)
            for ((i, item) in newShuffleList.withIndex()) {
                if (item > index) newShuffleList[i] -= 1
            }
            shuffleList = newShuffleList

            if (wasCurrent) {
                setCurrentPlaying(currentIndex, isPlaying)
            }
        }
    }

    private fun shufflePlayList(musicIndex: Int = shuffleList.getOrNull(currentIndex) ?: 0) {
        shuffleList = if (playMode == MusicPlayMode.RANDOM) {
            listOf(musicIndex) + (0 until musicList.size).filter { it != musicIndex }.shuffled()
        } else {
            List(musicList.size) { it }
        }
        currentIndex = shuffleList.indexOf(musicIndex)
    }

    private suspend fun setCurrentPlaying(index: Int, play: Boolean) = Coroutines.main {
        currentIndex = index
        val musicInfo = musicList[shuffleList[index]]
        val url = NSURL.fileURLWithPath(musicInfo.audioPath.toString())
        val media = VLCMedia.mediaWithURL(url)
        mediaPlayer?.media = media

        if (play) {
            mediaPlayer?.play()
        } else {
            mediaPlayer?.play() // 没找到预加载的方法, 先这样吧
            delay(100)
            mediaPlayer?.pause()
        }
    }

    private fun handleAudioSessionInterruption(type: AudioSessionInterruption, arg: Any?) {
        when (type) {
            AudioSessionInterruption.Began -> {
                isPlaying = false
            }
            AudioSessionInterruption.Ended -> {
                val shouldPlay = arg as Boolean
                if (isReady && shouldPlay) {
                    mediaPlayer?.play()
                }
            }
            AudioSessionInterruption.Failed -> {}
        }
    }

    private fun setupNowPlayingInfoCenter() {
        Coroutines.startMain {
            // 加上下面这行才能后台播放，否则后台切歌的时候会被suspend
            UIApplication.sharedApplication.beginReceivingRemoteControlEvents()
        }
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()
        commandCenter.playCommand.addTargetWithHandler { event ->
            Coroutines.startMain {
                play()
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.pauseCommand.addTargetWithHandler { event ->
            Coroutines.startMain {
                pause()
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.previousTrackCommand.addTargetWithHandler { event ->
            Coroutines.startMain {
                gotoPrevious()
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.nextTrackCommand.addTargetWithHandler { event ->
            Coroutines.startMain {
                gotoNext()
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val event = event as? MPChangePlaybackPositionCommandEvent
            Coroutines.startMain {
                event?.let {
                    seekTo((it.positionTime * 1000).roundToLong())
                }
            }
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun updatePlayingMetadata() {
        val nowPlayingInfoCenter = MPNowPlayingInfoCenter.defaultCenter()
        if (currentMusic == null) {
            nowPlayingInfoCenter.nowPlayingInfo = null
            return
        }
        val artwork = currentMusic?.let { MPMediaItemArtwork(UIImage(contentsOfFile = it.recordPath.toString())) }
        val nowPlayingInfo = mutableMapOf<Any?, Any?>()
        nowPlayingInfo[MPNowPlayingInfoPropertyAssetURL] = NSURL.fileURLWithPath(currentMusic?.audioPath?.toString()!!)
        nowPlayingInfo[MPNowPlayingInfoPropertyMediaType] = MPNowPlayingInfoMediaTypeAudio
        nowPlayingInfo[MPNowPlayingInfoPropertyIsLiveStream] = false
        nowPlayingInfo[MPNowPlayingInfoPropertyDefaultPlaybackRate] = 1.0
        nowPlayingInfo[MPMediaItemPropertyTitle] = currentMusic?.name
        nowPlayingInfo[MPMediaItemPropertyArtist] = currentMusic?.singer
        nowPlayingInfo[MPMediaItemPropertyComposer] = currentMusic?.composer
        nowPlayingInfo[MPMediaItemPropertyArtwork] = artwork
        nowPlayingInfo[MPMediaItemPropertyAlbumTitle] = currentMusic?.album
        nowPlayingInfoCenter.nowPlayingInfo = nowPlayingInfo
    }

    private fun updatePlayingInfo() {
        val nowPlayingInfoCenter = MPNowPlayingInfoCenter.defaultCenter()
        val nowPlayingInfo = nowPlayingInfoCenter.nowPlayingInfo?.toMutableMap() ?: mutableMapOf()
        nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = currentDuration / 1000.0f
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentPosition / 1000.0f
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = mediaPlayer?.rate
        nowPlayingInfoCenter.nowPlayingInfo = nowPlayingInfo
    }
}

@Stable
actual class MusicPlayer {
    // TODO: iOS端待实现
    actual val isInit: Boolean = false
    actual val isPlaying: Boolean = false
    actual val position: Long = 0L
    actual val duration: Long = 0L
    actual suspend fun init() {}
    actual suspend fun load(path: Path) {}
    actual fun play() {}
    actual fun pause() {}
    actual fun stop() {}
    actual fun release() {}
}