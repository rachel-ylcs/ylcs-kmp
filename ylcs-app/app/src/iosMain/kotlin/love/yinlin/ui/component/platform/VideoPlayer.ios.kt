package love.yinlin.ui.component.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import cocoapods.MobileVLCKit.*
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.compose.*
import love.yinlin.platform.Coroutines
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.platform.PlatformView
import love.yinlin.service
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNotification
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObject

@ExperimentalForeignApi
private class VideoPlayerView(var player: VLCMediaPlayer?) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    var isPlaying by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)

    private val playerDelegate: VLCMediaPlayerDelegateProtocol

    init {
        backgroundColor = UIColor.blackColor
        player?.drawable = this
        playerDelegate = object : NSObject(), VLCMediaPlayerDelegateProtocol {
            override fun mediaPlayerStateChanged(aNotification: NSNotification) = withPlayer { player ->
                if (player.state == VLCMediaPlayerState.VLCMediaPlayerStateEnded) {
                    Coroutines.startMain {
                        player.media = player.media?.url?.let { VLCMedia.mediaWithURL(it) }
                        player.play()
                    }
                } else {
                    isPlaying = player.playing
                    if (player.state == VLCMediaPlayerState.VLCMediaPlayerStatePlaying) {
                        duration = player.media?.length?.intValue?.toLong() ?: 0L
                    }
                }
            }

            override fun mediaPlayerTimeChanged(aNotification: NSNotification) = withPlayer { player ->
                position = player.time.intValue.toLong()
            }
        }
        player?.delegate = playerDelegate
    }

    fun release() {
        player?.stop()
        player?.delegate = null
        player?.drawable = null
        player = null
    }

    inline fun withPlayer(block: (player: VLCMediaPlayer) -> Unit) {
        player?.let(block)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onBack: () -> Unit
) {
    var wasMusicPlaying by rememberFalse()
    val state = rememberRefState<VideoPlayerView?> { null }

    if (service.config.audioFocus) {
        DisposableEffect(Unit) {
            wasMusicPlaying = service.musicFactory.instance.isPlaying
            if (wasMusicPlaying) {
                Coroutines.startMain {
                    service.musicFactory.instance.pause()
                }
            }
            onDispose {
                if (wasMusicPlaying) {
                    Coroutines.startMain {
                        service.musicFactory.instance.play()
                    }
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
        PlatformView(
            view = state,
            modifier = Modifier.fillMaxSize().zIndex(2f),
            factory = {
                val player = VLCMediaPlayer()
                val media = VLCMedia(url.let {
                    if (url.startsWith("http")) {
                        NSURL.URLWithString(it)!!
                    } else {
                        NSURL.fileURLWithPath(it)
                    }
                })
                player.media = media
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
                    it.withPlayer { player ->
                        if (player.isPlaying()) player.pause()
                        else player.play()
                    }
                },
                position = it.position,
                duration = it.duration,
                onProgressClick = { position ->
                    it.withPlayer { player -> player.time = VLCTime.timeWithInt(position.toInt()) }
                },
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