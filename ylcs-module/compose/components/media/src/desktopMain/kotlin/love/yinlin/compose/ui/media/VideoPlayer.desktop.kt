package love.yinlin.compose.ui.media

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import love.yinlin.annotation.NativeLibApi
import love.yinlin.foundation.PlatformContext
import love.yinlin.media.LinuxVideoController
import love.yinlin.media.MacOSVideoController
import love.yinlin.media.WindowsVideoController
import love.yinlin.platform.NativeLibLoader
import love.yinlin.platform.Platform
import love.yinlin.platform.platform
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode

@Stable
@NativeLibApi
abstract class DesktopVideoController(topBar: VideoActionBar.Factory, bottomBar: VideoActionBar.Factory) : VideoController(topBar, bottomBar) {
    companion object {
        init {
            NativeLibLoader.resource("media")
        }
    }

    private val paint = Paint().apply { this.isAntiAlias = true }

    var image: Bitmap? = null
        protected set

    protected var isRelease = false

    protected var nativeHandle: Long = nativeCreate()
        private set

    protected abstract fun nativeCreate(): Long
    protected abstract fun nativeRelease(handle: Long)

    override fun releaseController() {
        if (!isRelease) {
            isRelease = true
            nativeRelease(nativeHandle)
            nativeHandle = 0L
            image?.let {
                if (!it.isClosed) it.close()
            }
            image = null
            if (!paint.isClosed) paint.close()
        }
    }

    @Composable
    override fun SurfaceContent(modifier: Modifier) {
        Canvas(modifier = modifier) {
            // 利用 position 重组
            val image = if (position >= 0L) image else null
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            if (image != null && canvasHeight > 0f) {
                val canvasRatio = canvasWidth / canvasHeight
                val imageWidth = image.width.toFloat()
                val imageHeight = image.height.toFloat()
                val imageRatio = imageWidth / imageHeight
                Image.makeFromBitmap(image).use {
                    val dst = if (imageRatio > canvasRatio) {
                        val dstHeight = canvasWidth / imageRatio
                        Rect.makeXYWH(0f, (canvasHeight - dstHeight) / 2, canvasWidth, dstHeight)
                    }
                    else {
                        val dstWidth = canvasHeight * imageRatio
                        Rect.makeXYWH((canvasWidth - dstWidth) / 2, 0f, dstWidth, canvasHeight)
                    }
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawImageRect(
                            image = it,
                            src = Rect.makeWH(imageWidth, imageHeight),
                            dst = dst,
                            samplingMode = SamplingMode.MITCHELL,
                            paint = paint,
                            strict = false
                        )
                    }
                }
            }
        }
    }
}

actual fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory,
    bottomBar: VideoActionBar.Factory
): VideoController = when (platform) {
    Platform.Windows -> WindowsVideoController(topBar, bottomBar)
    Platform.MacOS -> MacOSVideoController(topBar, bottomBar)
    else -> LinuxVideoController(topBar, bottomBar)
}