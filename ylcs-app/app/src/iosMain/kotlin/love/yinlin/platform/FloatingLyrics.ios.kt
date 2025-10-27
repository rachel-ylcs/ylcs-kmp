package love.yinlin.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import cocoapods.YLCSCore.*
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import love.yinlin.AppEntry
import love.yinlin.compose.*
import love.yinlin.extension.toNSData
import love.yinlin.service
import org.jetbrains.skia.impl.use
import platform.AVFoundation.*
import platform.AVKit.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Stable
class ActualFloatingLyrics(controller: UIViewController) : FloatingLyrics() {

    private val composeScene by lazy {
        ImageComposeScene(
            width = CGRectGetWidth(pipView.frame).toInt(),
            height = CGRectGetHeight(pipView.frame).toInt(),
            content = {
                AppEntry(fill = false) {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                        Content()
                    }
                }
            }
        )
    }

    private var pipView = PIPView(CGRectMake(0.0, 0.0, 600.0, 150.0))

    private var pipDelegate = object : NSObject(), PictureInPictureDelegateProtocol {
        override fun didFailToStartPictureInPictureWithError(error: NSError) {
            service.config.enabledFloatingLyrics = false
        }

        override fun didStartPictureInPicture() {
            service.config.enabledFloatingLyrics = true
        }

        override fun didStopPictureInPicture() {
            service.config.enabledFloatingLyrics = false
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
        // TODO: 这里的转换layer是long, 应该是不能转换成功的, 是否应该用native的指针
        (pipView.layer as? AVSampleBufferDisplayLayer)?.flush()
        currentLyrics = lyrics
        composeScene.render().use { image ->
            image.encodeToData()?.use { imageData ->
                autoreleasepool {
                    val nsData = imageData.bytes.toNSData()
                    val uiImage = UIImage.imageWithData(nsData)
                    val buffer = uiImage?.asSampleBuffer()
                    (pipView.layer as? AVSampleBufferDisplayLayer)?.enqueueSampleBuffer(buffer)
                }
            }
        }
    }

    @Composable
    private fun Content() {
        val config = service.config.floatingLyricsIOSConfig
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
                    color = Colors(config.textColor),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(color = Colors(config.backgroundColor)).padding(CustomTheme.padding.value)
                )
            }
        }
    }
}