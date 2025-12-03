package love.yinlin.platform.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import cocoapods.YLCSCore.*
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import kotlinx.coroutines.delay
import love.yinlin.Context
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.graphics.PlatformImage
import love.yinlin.compose.graphics.encode
import love.yinlin.extension.toNSData
import org.jetbrains.skia.impl.use
import platform.AVFoundation.*
import platform.AVKit.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Stable
actual class FloatingLyrics {
    private val composeScene by lazy {
        ImageComposeScene(
            width = CGRectGetWidth(pipView.frame).toInt(),
            height = CGRectGetHeight(pipView.frame).toInt(),
            content = { Content() }
        )
    }

    private var pipView = PIPView(CGRectMake(0.0, 0.0, 600.0, 150.0))

    private var pipDelegate = object : NSObject(), PictureInPictureDelegateProtocol {
        override fun didFailToStartPictureInPictureWithError(error: NSError) {
            isAttached = false
        }

        override fun didStartPictureInPicture() {
            isAttached = true
        }

        override fun didStopPictureInPicture() {
            isAttached = false
            // iOS的画中画有关闭按钮
            if (isAttached != app.config.enabledFloatingLyrics)
                app.config.enabledFloatingLyrics = false
        }
    }

    val canAttached: Boolean get() = AVPictureInPictureController.isPictureInPictureSupported()

    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    actual fun attach() {
        update() // 必须先送一帧才能启动画中画
        pipView.pipController()?.startPictureInPicture()
    }

    actual fun detach() {
        pipView.pipController()?.stopPictureInPicture()
    }

    actual suspend fun initDelay(context: Context) {
        pipView.setHidden(true)
        pipView.setDelegate(pipDelegate)
        pipView.setupPIP()
        // 需要将画中画显示的View加入到控件树上
        context.controller.view.addSubview(pipView)

        if (app.config.enabledFloatingLyrics && !isAttached) {
            delay(1000L)
            attach()
        }
    }

    actual fun update() {
        (pipView.layer as? AVSampleBufferDisplayLayer)?.flush()
        composeScene.render().use { image ->
            PlatformImage(image).encode()?.toNSData()?.let { imageData ->
                autoreleasepool {
                    val uiImage = UIImage.imageWithData(imageData)
                    val buffer = uiImage?.asSampleBuffer()
                    (pipView.layer as? AVSampleBufferDisplayLayer)?.enqueueSampleBuffer(buffer)
                }
            }
        }
    }

    @Composable
    actual fun Content() {
        if (app.mp.isPlaying) {
            // TODO: 可不可以用app.Layout
            app.Layout {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    app.mp.engine.LyricsCanvas(config = app.config.lyricsEngineConfig, textStyle = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}