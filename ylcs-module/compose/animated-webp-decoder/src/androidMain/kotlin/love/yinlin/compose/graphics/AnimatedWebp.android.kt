package love.yinlin.compose.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toAndroidRectF
import love.yinlin.data.compose.ImageFormat
import love.yinlin.data.compose.ImageQuality
import love.yinlin.extension.catchingNull
import love.yinlin.extension.catchingThrow
import androidx.core.graphics.createBitmap

@Stable
actual class AnimatedWebp internal constructor(
    actual val width: Int,
    actual val height: Int,
    actual val frameCount: Int,
    private val row: Int,
    private val col: Int,
    private val paint: Paint,
    private val bitmap: Bitmap
) {
    actual fun DrawScope.drawFrame(index: Int, dst: Rect) {
        drawIntoCanvas { canvas ->
            val left = (index % col * width).toFloat()
            val top = (index / col * height).toFloat()
            val src = android.graphics.Rect(left.toInt(), top.toInt(), (left + width).toInt(), (top + height).toInt())
            canvas.nativeCanvas.drawBitmap(bitmap, src, dst.toAndroidRectF(), paint)
        }
    }

    actual fun encode(format: ImageFormat, quality: ImageQuality): ByteArray? = PlatformImage(bitmap).encode(format, quality)

    actual companion object {
        init {
            System.loadLibrary("webp_jni")
        }

        actual fun decode(data: ByteArray): AnimatedWebp? = catchingNull {
            var handle = 0L
            var iteratorHandle = 0L
            catchingThrow(
                clean = {
                    nativeAnimatedWebpReleaseIterator(iteratorHandle)
                    nativeAnimatedWebpRelease(handle)
                }
            ) {
                handle = nativeAnimatedWebpCreate(data)
                require(handle != 0L)
                val width = nativeAnimatedWebpGetWidth(handle)
                val height = nativeAnimatedWebpGetHeight(handle)
                val frameCount = nativeAnimatedWebpGetFrameCount(handle)
                require(width > 0 && height > 0 && frameCount > 1)

                val (row, col) = calculateGrid(width, height, frameCount)
                require(row > 0 && col > 0)

                val mergeWidth = col * width
                val mergeHeight = row * height
                val mergeBitmap = createBitmap(mergeWidth, mergeHeight, Bitmap.Config.ARGB_8888)

                val paint = Paint()
                paint.isAntiAlias = true

                val canvas = Canvas(mergeBitmap)

                var count = 0
                iteratorHandle = nativeAnimatedWebpCreateIterator()
                var result = nativeAnimatedWebpFirstFrame(handle, iteratorHandle)
                require(result)
                while (result) {
                    ++count
                    // do
                    result = nativeAnimatedWebpNextFrame(iteratorHandle)
                }
                require(count == frameCount)

                AnimatedWebp(width, height, frameCount, row, col, paint, mergeBitmap)
            }
        }
    }
}