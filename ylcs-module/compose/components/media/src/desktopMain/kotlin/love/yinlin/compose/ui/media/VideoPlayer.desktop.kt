package love.yinlin.compose.ui.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.zIndex
import love.yinlin.annotation.NativeLibApi
import love.yinlin.compose.Colors
import love.yinlin.foundation.Context
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode

@Composable
@NativeLibApi
actual fun VideoPlayer(controller: VideoController, modifier: Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize().background(Colors.Black).zIndex(1f)) {
            // 利用 updateCount 重组
            val image = if (controller.updateCount > 0L) controller.image else null
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

        controller.VideoPlayerControls(modifier = Modifier.matchParentSize().zIndex(2f))
    }
}

actual fun buildVideoController(
    context: Context,
    topBar: VideoActionBar?,
    bottomBar: VideoActionBar?
): VideoController = VideoController.build(context, topBar, bottomBar)