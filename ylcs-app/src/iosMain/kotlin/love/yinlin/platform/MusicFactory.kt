package love.yinlin.platform

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import kotlinx.cinterop.*
import kotlin.math.roundToLong
import love.yinlin.ui.screen.music.audioPath
import love.yinlin.ui.screen.music.recordPath
import platform.darwin.*
import platform.Foundation.NSKeyValueObservingProtocol
import platform.Foundation.*
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.AVFAudio.*
import platform.MediaPlayer.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
class ActualMusicFactory : MusicFactory() {
    private var interruptionObserver: NSObjectProtocol? = null

    private var avPlayer: AVPlayer? = null
    private var currentIndex = -1
    private lateinit var playerObserver: NSObject
    private var timeObserver: Any? = null
    private var endTimeObserver: NSObjectProtocol? = null

    override val isInit: Boolean get() = avPlayer != null
    override var error: Throwable? by mutableStateOf(null)
    override var playMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
    override var musicList: List<MusicInfo> by mutableStateOf(emptyList())
    override val isReady: Boolean by derivedStateOf { musicList.isNotEmpty() }
    override var isPlaying: Boolean by mutableStateOf(false)
    override var currentPosition: Long by mutableLongStateOf(0L)
    override var currentDuration: Long by mutableLongStateOf(0L)
    override var currentMusic: MusicInfo? by mutableStateOf(null)

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
                    val userInfo = notification?.userInfo
                    userInfo?.let {
                        val interruptionType = it[AVAudioSessionInterruptionTypeKey] as Long
                        when (interruptionType.toULong()) {
                            AVAudioSessionInterruptionTypeBegan -> {
                                handleAudioSessionInterruption(AudioSessionInterruption.Began, null)
                            }
                            AVAudioSessionInterruptionTypeEnded -> {
                                val options = userInfo[AVAudioSessionInterruptionOptionKey] as UInt
                                val shouldResume = options and AVAudioSessionInterruptionFlags_ShouldResume
                                handleAudioSessionInterruption(AudioSessionInterruption.Ended, shouldResume)
                            }
                            else -> {
                                handleAudioSessionInterruption(AudioSessionInterruption.Failed, null)
                            }
                        }
                    }
                }
                setCategory(AVAudioSessionCategoryPlayback, null)
                setActive(true, null)
                setupNowPlayingInfoCenter()
            } catch (e: Throwable) {
                error = e
            }
        }

        playerObserver = object : NSObject(), NSKeyValueObservingProtocol {
            override fun observeValueForKeyPath(
                keyPath: String?,
                ofObject: Any?,
                change: Map<Any?, *>?,
                context: COpaquePointer?
            ) {
                val newValue = change!![NSKeyValueChangeNewKey]
                when (keyPath) {
                    "timeControlStatus" -> isPlaying = newValue == AVPlayerTimeControlStatusPlaying
                    "currentItem" -> {
                        if (newValue == null) currentDuration = 0L
                        updatePlayingMetadata()
                    }
                    "status" -> if (newValue == AVPlayerItemStatusReadyToPlay)
                        avPlayer?.currentItem?.let {
                            currentDuration = CMTimeGetSeconds(it.duration).toLong() * 1000
                            updatePlayingInfo()
                        }
                    else -> println("Unknown observed $keyPath:$newValue")
                }
            }
        }

        avPlayer = AVPlayer()
        avPlayer?.addObserver(playerObserver, "timeControlStatus", NSKeyValueObservingOptionNew, null)
        avPlayer?.addObserver(playerObserver, "currentItem", NSKeyValueObservingOptionNew, null)

        timeObserver = avPlayer?.addPeriodicTimeObserverForInterval(
            CMTimeMake(1, 2), // 0.5秒
            dispatch_get_main_queue()
        ) { time ->
            if (isPlaying) {
                currentPosition = (CMTimeGetSeconds(time) * 1000).toLong()
                updatePlayingInfo()
            }
        }
    }

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) {
        playMode = musicPlayMode
    }

    override suspend fun play(): Unit = Coroutines.main {
        avPlayer?.play()
    }

    override suspend fun pause(): Unit = Coroutines.main {
        avPlayer?.pause()
    }

    override suspend fun stop(): Unit = Coroutines.main {
        avPlayer?.pause()
        avPlayer?.seekToTime(CMTimeMake(0, 1))
        setPlayerItem(null)
        currentPosition = 0L
        musicList = emptyList()
        currentPlaylist = null
        currentMusic = null
        currentIndex = -1
        updatePlayingMetadata()
    }

    override suspend fun gotoPrevious() {
        if (musicList.isEmpty()) return

        val newIndex = when {
            currentIndex <= 0 -> musicList.size - 1
            else -> currentIndex - 1
        }
        gotoIndex(newIndex)
    }

    override suspend fun gotoNext() {
        if (musicList.isEmpty()) return

        val newIndex = when (playMode) {
            MusicPlayMode.RANDOM -> (0 until musicList.size).random()
            else -> (currentIndex + 1) % musicList.size
        }
        gotoIndex(newIndex)
    }

    override suspend fun gotoIndex(index: Int) {
        if (index < 0 || index >= musicList.size) return

        setCurrentPlaying(index, true)
        currentPosition = 0L
    }

    override suspend fun seekTo(position: Long): Unit = Coroutines.main {
        val time = CMTimeMakeWithSeconds(position / 1000.0, 1000)
        val tolerance = CMTimeMake(0, 1)
        avPlayer?.seekToTime(time, tolerance, tolerance) { finished ->
            if (finished) {
                avPlayer?.currentTime()?.let { actualTime ->
                    currentPosition = (CMTimeGetSeconds(actualTime) * 1000).toLong()
                }
            }
        }
    }

    override suspend fun prepareMedias(
        medias: List<MusicInfo>, startIndex: Int?, playing: Boolean
    ) {
        musicList = medias

        if (medias.isNotEmpty()) {
            setCurrentPlaying(startIndex?.coerceIn(0, musicList.size - 1) ?: 0, true)
        }
        currentPosition = 0L
    }

    override suspend fun addMedias(medias: List<MusicInfo>) {
        if (medias.isEmpty()) return

        val oldSize = musicList.size
        musicList = musicList + medias

        if (oldSize == 0 && musicList.isNotEmpty()) {
            setCurrentPlaying(0, false)
        }
    }

    override suspend fun removeMedia(index: Int) {
        if (index < 0 || index >= musicList.size) return

        Coroutines.main {
            val wasCurrent = index == currentIndex
            musicList = musicList.filterIndexed { id, _ -> id != index }

            when {
                musicList.isEmpty() -> {
                    currentIndex = -1
                    currentMusic = null
                    setPlayerItem(null)
                }
                wasCurrent -> {
                    setCurrentPlaying(currentIndex.coerceAtMost(musicList.size - 1), isPlaying)
                }
                index < currentIndex -> {
                    currentIndex--
                }
            }
        }
    }

    private suspend fun setCurrentPlaying(index: Int, play: Boolean) = Coroutines.main {
        currentIndex = index
        currentMusic = musicList[index]
        setPlayerItem(currentMusic)

        if (play) {
            avPlayer?.play()
        }
    }

    private fun setPlayerItem(musicInfo: MusicInfo?) {
        avPlayer?.currentItem?.removeObserver(playerObserver, "status")
        val playerItem = musicInfo?.let {
            val url = NSURL.fileURLWithPath(it.audioPath.toString())
            val asset = AVURLAsset.URLAssetWithURL(url, mapOf(
                // https://stackoverflow.com/questions/9290972
                "AVURLAssetOutOfBandMIMETypeKey" to "audio/flac",
                AVURLAssetPreferPreciseDurationAndTimingKey to true
            ))
            AVPlayerItem.playerItemWithAsset(asset).let {
                it.addObserver(playerObserver, "status", NSKeyValueObservingOptionNew, null)
                it
            }
        }
        avPlayer?.replaceCurrentItemWithPlayerItem(playerItem)
        setupEndTimeObserver()
    }

    private fun setupEndTimeObserver() {
        endTimeObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
            endTimeObserver = null
        }

        avPlayer?.currentItem?.let {
            endTimeObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                AVPlayerItemDidPlayToEndTimeNotification, it, null) { _ ->
                Coroutines.startMain {
                    when (playMode) {
                        MusicPlayMode.LOOP -> {
                            avPlayer?.seekToTime(CMTimeMake(0, 1))
                            avPlayer?.play()
                        }
                        else -> {
                            gotoNext()
                        }
                    }
                }
            }
        }
    }

    private fun handleAudioSessionInterruption(type: AudioSessionInterruption, arg: Any?) {
        when (type) {
            AudioSessionInterruption.Began -> {}
            AudioSessionInterruption.Ended -> {
                val shouldPlay = arg as Boolean
                if (isReady && shouldPlay) {
                    avPlayer?.play()
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
        var nowPlayingInfo = mutableMapOf<Any?, Any?>()
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
        var nowPlayingInfo = nowPlayingInfoCenter.nowPlayingInfo?.toMutableMap() ?: mutableMapOf()
        nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = currentDuration / 1000.0f
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentPosition / 1000.0f
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = avPlayer?.rate
        nowPlayingInfoCenter.nowPlayingInfo = nowPlayingInfo
    }
}