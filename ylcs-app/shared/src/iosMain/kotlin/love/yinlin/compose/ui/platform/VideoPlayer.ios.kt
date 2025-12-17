package love.yinlin.compose.ui.platform

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
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNotification
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObject

@Stable
@OptIn(ExperimentalForeignApi::class)
private class VideoPlayerWrapper : PlatformView<UIView>() {
    private val player = VLCMediaPlayer()
    var isPlaying by mutableStateOf(false)
        private set
    var position by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(0L)
        private set

    override fun build(): UIView {
        val uiView = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))
        uiView.backgroundColor = UIColor.blackColor
        player.drawable = uiView
        player.delegate = object : NSObject(), VLCMediaPlayerDelegateProtocol {
            override fun mediaPlayerStateChanged(aNotification: NSNotification) {
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

            override fun mediaPlayerTimeChanged(aNotification: NSNotification) {
                position = player.time.intValue.toLong()
            }
        }
        return uiView
    }

    override fun release(view: UIView) {
        player.stop()
        player.drawable = null
        player.delegate = null
    }

    fun setMedia(url: String) {
        val media = VLCMedia(url.let {
            if (url.startsWith("http")) {
                NSURL.URLWithString(it)!!
            } else {
                NSURL.fileURLWithPath(it)
            }
        })
        player.media = media
        player.play()
    }

    fun playOrPause() {
        if (player.isPlaying()) player.pause()
        else player.play()
    }

    fun playOrPause(isForeground: Boolean) {
        if (isForeground) player.play()
        else player.pause()
    }

    fun seekTo(position: Long) {
        player.time = VLCTime.timeWithInt(position.toInt())
    }
}

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val wrapper = remember { VideoPlayerWrapper() }

    LaunchedEffect(url) { wrapper.setMedia(url) }

    OffScreenEffect { wrapper.playOrPause(it) }

    Box(modifier = modifier) {
        Box(Modifier.matchParentSize().background(Colors.Black).zIndex(1f))
        wrapper.Content(Modifier.fillMaxSize().zIndex(2f))
        VideoPlayerControls(
            modifier = Modifier.fillMaxSize().zIndex(3f),
            isPlaying = wrapper.isPlaying,
            onPlayClick = { wrapper.playOrPause() },
            position = wrapper.position,
            duration = wrapper.duration,
            onProgressClick = { wrapper.seekTo(it) },
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