package love.yinlin.media

import androidx.compose.runtime.Stable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import love.yinlin.coroutines.mainContext
import love.yinlin.extension.catching
import love.yinlin.extension.catchingDefault
import love.yinlin.foundation.PlatformContext
import platform.AVFAudio.*
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeMake
import platform.Foundation.*
import platform.MediaPlayer.*
import platform.UIKit.*
import platform.darwin.NSObjectProtocol

// TODO: 仍然还有很多需要修改的，包括进度、时长、状态、音源变更的监听等等
@Stable
@OptIn(ExperimentalForeignApi::class)
class IOSMusicPlayer(fetcher: MediaMetadataFetcher) : CommonMusicPlayer(fetcher) {
    @Stable
    private enum class AudioSessionInterruption { Began, Ended, Failed; }

    private val scope = CoroutineScope(SupervisorJob() + mainContext)
    private val player: AVPlayer = AVPlayer()
    private val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()
    private var interruptionObserver: NSObjectProtocol? = null

    private fun handleAudioSessionInterruption(type: AudioSessionInterruption, arg: Any?) {
        when (type) {
            AudioSessionInterruption.Began -> isPlaying = false
            AudioSessionInterruption.Ended -> {
                val shouldPlay = arg as? Boolean == true
                if (isReady && shouldPlay) player.play()
            }
            AudioSessionInterruption.Failed -> {}
        }
    }

    override suspend fun init(context: PlatformContext) {
        isInit = catchingDefault(false) {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(
                category = AVAudioSessionCategoryPlayback,
                error = null,
                withOptions = if (fetcher.audioFocus) 0UL else AVAudioSessionCategoryOptionMixWithOthers,
            )
            session.setActive(true, error = null)

            // 加上下面这行才能后台播放，否则后台切歌的时候会被suspend
            UIApplication.sharedApplication.beginReceivingRemoteControlEvents()
            commandCenter.playCommand.addTargetWithHandler {
                scope.launch { play() }
                MPRemoteCommandHandlerStatusSuccess
            }
            commandCenter.pauseCommand.addTargetWithHandler {
                scope.launch { pause() }
                MPRemoteCommandHandlerStatusSuccess
            }
            commandCenter.previousTrackCommand.addTargetWithHandler {
                scope.launch { gotoPrevious() }
                MPRemoteCommandHandlerStatusSuccess
            }
            commandCenter.nextTrackCommand.addTargetWithHandler {
                scope.launch { gotoNext() }
                MPRemoteCommandHandlerStatusSuccess
            }
            commandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
                val event = event as? MPChangePlaybackPositionCommandEvent
                if (event != null) {
                    scope.launch { seekTo((event.positionTime * 1000).toLong()) }
                }
                MPRemoteCommandHandlerStatusSuccess
            }

            interruptionObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                AVAudioSessionInterruptionNotification, this, NSOperationQueue.mainQueue
            ) { notification ->
                notification?.userInfo?.let { userInfo ->
                    val interruptionType = userInfo[AVAudioSessionInterruptionTypeKey] as Long
                    when (interruptionType.toULong()) {
                        AVAudioSessionInterruptionTypeBegan -> handleAudioSessionInterruption(AudioSessionInterruption.Began, null)
                        AVAudioSessionInterruptionTypeEnded -> {
                            val options = userInfo[AVAudioSessionInterruptionOptionKey] as Long
                            val shouldResume = (options.toULong() and AVAudioSessionInterruptionOptionShouldResume) != 0UL
                            handleAudioSessionInterruption(AudioSessionInterruption.Ended, shouldResume)
                        }
                        else -> handleAudioSessionInterruption(AudioSessionInterruption.Failed, null)
                    }
                }
            }

            true
        }
    }

    override fun release() {
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        catching { AVAudioSession.sharedInstance().setActive(false, error = null) }
        interruptionObserver = null
        scope.cancel()
    }

    override suspend fun play() {
        if (isReady) player.play()
    }

    override suspend fun pause() {
        if (isReady) player.pause()
    }

    override suspend fun seekTo(position: Long) {
        if (isReady) player.seekToTime(CMTimeMake(position, 1000))
    }

    override fun innerStop() {
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
    }

    override fun innerGotoIndex(path: String, playing: Boolean): Boolean {
        val nsUrl = if (path.startsWith("http")) NSURL.URLWithString(path) else NSURL.fileURLWithPath(path)
        return if (nsUrl != null) {
            player.replaceCurrentItemWithPlayerItem(AVPlayerItem(nsUrl))
            if (playing) player.play()
            true
        } else false
    }
}

actual fun buildMusicPlayer(fetcher: MediaMetadataFetcher): MusicPlayer = IOSMusicPlayer(fetcher)