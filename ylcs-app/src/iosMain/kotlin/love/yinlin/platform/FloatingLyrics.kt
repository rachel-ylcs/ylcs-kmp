package love.yinlin.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import cocoapods.YLCSCore.*
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.DeviceWrapper
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.ThemeValue
import love.yinlin.common.toNSData
import platform.darwin.NSObject
import platform.Foundation.*
import platform.CoreGraphics.*
import platform.UIKit.*
import platform.AVFoundation.*
import platform.AVKit.*

@OptIn(ExperimentalForeignApi::class)
@Stable
class ActualFloatingLyrics(private val controller: UIViewController) : FloatingLyrics() {

    private var pipView = PIPView(CGRectMake(0.0, 0.0, 600.0, 150.0))

    private var pipDelegate = object : NSObject(), PictureInPictureDelegateProtocol {
        override fun didFailToStartPictureInPictureWithError(error: NSError) {
            app.config.enabledFloatingLyrics = false
        }

        override fun didStartPictureInPicture() {
            app.config.enabledFloatingLyrics = true
        }

        override fun didStopPictureInPicture() {
            app.config.enabledFloatingLyrics = false
        }
    }

    init {
        pipView.setHidden(true)
        pipView.setDelegate(pipDelegate)
        pipView.setupPIP()
        // 需要将画中画显示的View加入到控件树上
        controller.view.addSubview(pipView)
    }

    private var currentLyrics: String? by mutableStateOf(null)

    val canAttached: Boolean get() = AVPictureInPictureController.isPictureInPictureSupported()

    override val isAttached: Boolean get() = pipView.pipController()?.isPictureInPictureActive() ?: false

    fun attach() {
        updateLyrics("") // 必须先送一帧才能启动画中画
        pipView.pipController()?.startPictureInPicture()
    }

    fun detach() {
        pipView.pipController()?.stopPictureInPicture()
    }

    override fun updateLyrics(lyrics: String?) {
        (pipView.layer as? AVSampleBufferDisplayLayer)?.flush()
        currentLyrics = lyrics
        val imageData = renderComposeScene(
            CGRectGetWidth(pipView.frame).toInt(),
            CGRectGetHeight(pipView.frame).toInt(),
            content = {
                ContentWrapper()
            }
        ).encodeToData()
        val nsData = imageData?.bytes?.toNSData()
        val uiImage = nsData?.let { UIImage.imageWithData(it) }
        val buffer = uiImage?.asSampleBuffer()
        (pipView.layer as? AVSampleBufferDisplayLayer)?.enqueueSampleBuffer(buffer)
    }

    @Composable
    private fun ContentWrapper() {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            DeviceWrapper(
                device = remember(this.maxWidth) { Device(this.maxWidth) },
                themeMode = app.config.themeMode,
                fontScale = 1f
            ) {
                Content()
            }
        }
    }

    @Composable
    fun Content() {
        val config = app.config.floatingLyricsIOSConfig
        currentLyrics?.let { lyrics ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lyrics,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = MaterialTheme.typography.labelLarge.fontSize * config.textSize
                    ),
                    color = Colors.from(config.textColor),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(color = Colors.from(config.backgroundColor)).padding(ThemeValue.Padding.Value)
                )
            }
        }
    }
}