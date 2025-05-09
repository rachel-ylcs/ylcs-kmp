package love.yinlin.platform

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import kotlinx.cinterop.*
import love.yinlin.ui.screen.music.audioPath
import platform.darwin.*
import platform.Foundation.NSKeyValueObservingProtocol
import platform.Foundation.*
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.AVFAudio.*

@OptIn(ExperimentalForeignApi::class)
class ActualMusicFactory : MusicFactory() {
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

    override suspend fun init() {
        if (isInit) return

        AVAudioSession.sharedInstance().apply {
            try {
                setCategory(AVAudioSessionCategoryPlayback, null)
                setActive(true, null)
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
                    "currentItem" -> if (newValue == null) currentDuration = 0L
                    "status" -> if (newValue == AVPlayerItemStatusReadyToPlay)
                        avPlayer?.currentItem?.let {
                            currentDuration = CMTimeGetSeconds(it.duration).toLong() * 1000
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
            currentPosition = (CMTimeGetSeconds(time) * 1000).toLong()
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
        currentPosition = 0L
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

        setupEndTimeObserver()

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
                AVURLAssetPreferPreciseDurationAndTimingKey to 1
            ))
            AVPlayerItem.playerItemWithAsset(asset).let {
                it.addObserver(playerObserver, "status", NSKeyValueObservingOptionNew, null)
                it
            }
        }
        avPlayer?.replaceCurrentItemWithPlayerItem(playerItem)
    }

    private fun setupEndTimeObserver() {
        endTimeObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }

        endTimeObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            avPlayer?.currentItem,
            null
        ) { _ ->
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