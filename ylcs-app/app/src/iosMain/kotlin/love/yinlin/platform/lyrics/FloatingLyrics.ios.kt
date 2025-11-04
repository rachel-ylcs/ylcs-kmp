package love.yinlin.platform.lyrics

import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import cocoapods.YLCSCore.*
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import love.yinlin.Context
import love.yinlin.app
import love.yinlin.compose.*
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
    // TODO: 需要参照其他平台 review
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

    val canAttached: Boolean get() = AVPictureInPictureController.isPictureInPictureSupported()

    actual val isAttached: Boolean get() = pipView.pipController()?.isPictureInPictureActive() ?: false

    actual fun attach() {
        updateLyrics("") // 必须先送一帧才能启动画中画
        pipView.pipController()?.startPictureInPicture()
    }

    actual fun detach() {
        pipView.pipController()?.stopPictureInPicture()
    }

    fun updateLyrics(lyrics: String?) {
        // TODO: 这里的转换layer是long, 应该是不能转换成功的, 是否应该用native的指针
        (pipView.layer as? AVSampleBufferDisplayLayer)?.flush()
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

    actual suspend fun initDelay(context: Context) {

    }

    @Composable
    actual fun Content() {
        app.Layout {
            with(app.mp.engine) {
                Content(config = app.config.lyricsEngineConfig)
            }
        }
    }
}