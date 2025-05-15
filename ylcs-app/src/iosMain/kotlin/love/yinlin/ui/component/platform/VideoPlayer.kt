package love.yinlin.ui.component.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.common.Colors
import love.yinlin.extension.OffScreenEffect
import love.yinlin.extension.rememberState
import love.yinlin.platform.Coroutines
import love.yinlin.platform.app
import love.yinlin.ui.CustomUI
import love.yinlin.ui.component.image.ClickIcon
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.*
import platform.Foundation.*
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

val AVPlayer.isPlaying: Boolean
    get() = timeControlStatus == AVPlayerTimeControlStatusPlaying

@ExperimentalForeignApi
private class VideoPlayerView(var player: AVPlayer?) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    var isPlaying by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)

    private val playerLayer: AVPlayerLayer
    private val playerObserver: NSObject
    private var timeObserver: Any? = null

    init {
        backgroundColor = UIColor.blackColor
        playerLayer = AVPlayerLayer()
        playerLayer.player = player
        layer.addSublayer(playerLayer)
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
                    "status" -> if (newValue == AVPlayerItemStatusReadyToPlay)
                        player?.currentItem?.let {
                            duration = CMTimeGetSeconds(it.duration).toLong() * 1000
                        }
                    else -> println("Unknown observed $keyPath:$newValue")
                }
            }
        }
        player?.addObserver(playerObserver, "timeControlStatus", NSKeyValueObservingOptionNew, null)
        player?.currentItem?.addObserver(playerObserver, "status", NSKeyValueObservingOptionNew, null)
        addTimeObserver()
    }

    fun release() {
        player?.pause()
        removeTimeObserver()
        player?.removeObserver(playerObserver, "timeControlStatus")
        player?.currentItem?.removeObserver(playerObserver, "status")
        player = null
    }

    fun seek(pos: Long) {
        val time = CMTimeMakeWithSeconds(pos / 1000.0, 1000)
        val tolerance = CMTimeMake(0, 1)
        removeTimeObserver()
        position = pos
        player?.seekToTime(time, tolerance, tolerance) { finished ->
            if (finished) {
                player?.currentTime()?.let { actualTime ->
                    position = (CMTimeGetSeconds(actualTime) * 1000).toLong()
                }
                addTimeObserver()
            }
        }
    }

    private fun addTimeObserver() {
        timeObserver = player?.addPeriodicTimeObserverForInterval(
            CMTimeMake(1, 2), // 0.5秒
            dispatch_get_main_queue()
        ) { time ->
            position = (CMTimeGetSeconds(time) * 1000).toLong()
        }
    }

    private fun removeTimeObserver() {
        timeObserver?.let {
            player?.removeTimeObserver(it)
            timeObserver = null
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        playerLayer.frame = bounds
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val wasMusicPlaying = rememberState { false }
    val state: MutableState<VideoPlayerView?> = rememberState { null }

    DisposableEffect(Unit) {
        wasMusicPlaying.value = app.musicFactory.isPlaying
        if (wasMusicPlaying.value) {
            Coroutines.startMain {
                app.musicFactory.pause()
            }
        }
        onDispose {
            if (wasMusicPlaying.value) {
                Coroutines.startMain {
                    app.musicFactory.play()
                }
            }
        }
    }

    OffScreenEffect { isForeground ->
        state.value?.player?.let {
            if (isForeground) it.play()
            else it.pause()
        }
    }

    Box(modifier = modifier) {
        Box(Modifier.matchParentSize().background(Colors.Black).zIndex(1f))
        CustomUI(
            view = state,
            modifier = Modifier.fillMaxSize().zIndex(2f),
            factory = {
                var player: AVPlayer
                if (url.startsWith("http")) {
                    val videoURL = NSURL.URLWithString(url)!!
                    player = AVPlayer(videoURL)
                } else {
                    val fileUrl = NSURL.fileURLWithPath(url)
                    val asset = AVURLAsset.URLAssetWithURL(fileUrl, mapOf(
                        "AVURLAssetOutOfBandMIMETypeKey" to "video/mp4",
                    ))
                    val playerItem = AVPlayerItem.playerItemWithAsset(asset)
                    player = AVPlayer(playerItem)
                }
                player.play()
                VideoPlayerView(player)
            },
            release = { player, onRelease ->
                player.release()
                onRelease()
            }
        )
        state.value?.let {
            VideoPlayerControls(
                modifier = Modifier.fillMaxSize().zIndex(3f),
                isPlaying = it.isPlaying,
                onPlayClick = {
                    it.player?.let { player ->
                        if (player.isPlaying) player.pause()
                        else player.play()
                    }
                },
                position = it.position,
                duration = it.duration,
                onProgressClick = { position -> it.seek(position) },
                topBar = {
                    ClickIcon(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        color = Colors.White,
                        onClick = onBack
                    )
                }
            )
        }
    }
}