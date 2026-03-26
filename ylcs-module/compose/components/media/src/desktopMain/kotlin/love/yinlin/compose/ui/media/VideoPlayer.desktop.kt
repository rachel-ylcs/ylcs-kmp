package love.yinlin.compose.ui.media

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import love.yinlin.annotation.NativeLibApi
import love.yinlin.foundation.PlatformContext
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode

actual fun buildVideoController(
    context: PlatformContext,
    topBar: VideoActionBar.Factory,
    bottomBar: VideoActionBar.Factory
): VideoController = VideoController.build(topBar, bottomBar)

@Composable
@NativeLibApi
actual fun VideoSurface(controller: VideoController, modifier: Modifier) {
    Canvas(modifier = modifier) {
        // 利用 position 重组
        val image = if (controller.position >= 0L) controller.image else null
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
                } else {
                    val dstWidth = canvasHeight * imageRatio
                    Rect.makeXYWH((canvasWidth - dstWidth) / 2, 0f, dstWidth, canvasHeight)
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawImageRect(
                        image = it,
                        src = Rect.makeWH(imageWidth, imageHeight),
                        dst = dst,
                        samplingMode = SamplingMode.MITCHELL,
                        paint = controller.paint,
                        strict = false
                    )
                }
            }
        }
    }
}