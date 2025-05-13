package love.yinlin.ui.component.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.common.Colors
import love.yinlin.extension.clickableNoRipple
import love.yinlin.extension.rememberState
import love.yinlin.ui.CustomUI
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.UIView

val AVPlayer.isPlaying: Boolean
    get() = rate > 0.0

@ExperimentalForeignApi
private class VideoPlayerView(var player: AVPlayer?) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val playerLayer: AVPlayerLayer

    init {
        backgroundColor = UIColor.blackColor
        playerLayer = AVPlayerLayer()
        playerLayer.player = player
        layer.addSublayer(playerLayer)
    }

    fun release() {
        player = null
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
    onBack: () -> Unit,
    onDownload: () -> Unit
) {
    val player: MutableState<VideoPlayerView?> = rememberState { null }

    LifecycleStartEffect(Unit) {
        player.value?.player?.let {
            if (!it.isPlaying) it.play()
        }
        onStopOrDispose {
            player.value?.player?.let {
                if (it.isPlaying) it.pause()
            }
        }
    }

    Box(modifier = modifier) {
        Box(Modifier.matchParentSize().background(Colors.Black).zIndex(1f))
        CustomUI(
            view = player,
            modifier = Modifier.fillMaxSize().zIndex(2f),
            factory = {
                var player: AVPlayer
                if (url.startsWith("http")) {
                    val videoURL = NSURL.URLWithString(url)!!
                    player = AVPlayer(videoURL)
                } else {
                    val url = NSURL.fileURLWithPath(url)
                    val asset = AVURLAsset.URLAssetWithURL(url, mapOf(
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
        Box(modifier = Modifier.fillMaxSize().clickableNoRipple {
            player.value?.player?.let {
                if (it.isPlaying) it.pause()
                else it.play()
            }
        }.zIndex(3f))
    }
}