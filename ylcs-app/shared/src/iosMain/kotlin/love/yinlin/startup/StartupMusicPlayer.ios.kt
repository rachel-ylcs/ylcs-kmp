package love.yinlin.startup

import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cocoapods.MobileVLCKit.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.io.files.Path
import love.yinlin.foundation.StartupFetcher
import love.yinlin.app
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.extension.catchingError
import love.yinlin.foundation.Context
import platform.darwin.*
import platform.Foundation.*
import platform.AVFAudio.*
import platform.MediaPlayer.*
import platform.UIKit.*
import kotlin.collections.set
import kotlin.math.roundToLong

@Stable
private enum class AudioSessionInterruption {
    Began, Ended, Failed;
}

@StartupFetcher(index = 0, name = "rootPath", returnType = Path::class, nullable = true)
@OptIn(ExperimentalForeignApi::class)
@Stable
actual fun buildMusicPlayer(): StartupMusicPlayer = object : StartupMusicPlayer() {
    private var mediaPlayer: VLCMediaPlayer? by mutableStateOf(null)
    private var interruptionObserver: NSObjectProtocol? = null
    private var playerDelegate: VLCMediaPlayerDelegateProtocol? = null
    private var shuffleList = listOf<Int>()
    private var currentIndex = -1

    override val isInit: Boolean by derivedStateOf { mediaPlayer != null }
    override var error: Throwable? by mutableRefStateOf(null)
    override var playMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
    override var musicList: List<MusicInfo> by mutableRefStateOf(emptyList())
    override val isReady: Boolean by derivedStateOf { musicList.isNotEmpty() }
    override var isPlaying: Boolean by mutableStateOf(false)
    override var currentDuration: Long by mutableLongStateOf(0L)
    override var currentPosition: Long by mutableLongStateOf(0L)
    override var currentMusic: MusicInfo? by mutableRefStateOf(null)

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
        playlist = null
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
        if (index in shuffleList.indices) {
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
        if (index !in musicList.indices) return

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

    override suspend fun initController(context: Context) {
        if (isInit) return

        AVAudioSession.sharedInstance().apply {
            catchingError {
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
            }?.let { error = it }
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

    private fun shufflePlayList(musicIndex: Int = shuffleList.getOrNull(currentIndex) ?: 0) {
        shuffleList = if (playMode == MusicPlayMode.RANDOM) {
            listOf(musicIndex) + musicList.indices.filter { it != musicIndex }.shuffled()
        } else {
            List(musicList.size) { it }
        }
        currentIndex = shuffleList.indexOf(musicIndex)
    }

    private suspend fun setCurrentPlaying(index: Int, play: Boolean) = Coroutines.main {
        currentIndex = index
        val musicInfo = musicList[shuffleList[index]]
        val url = NSURL.fileURLWithPath(musicInfo.path(ModResourceType.Audio).toString())
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
        val artwork = currentMusic?.let { MPMediaItemArtwork(UIImage(contentsOfFile = it.path(ModResourceType.Record).toString())) }
        val nowPlayingInfo = mutableMapOf<Any?, Any?>()
        nowPlayingInfo[MPNowPlayingInfoPropertyAssetURL] = NSURL.fileURLWithPath(currentMusic?.path(ModResourceType.Audio).toString())
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