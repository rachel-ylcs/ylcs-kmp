package love.yinlin.compose.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toAndroidRectF
import love.yinlin.data.compose.ImageFormat
import love.yinlin.data.compose.ImageQuality
import love.yinlin.extension.catchingNull
import love.yinlin.extension.catchingThrow
import androidx.core.graphics.createBitmap
import java.nio.ByteBuffer

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
    actual fun DrawScope.drawFrame(index: Int, dst: Rect, alpha: Float, filter: ColorFilter?, blendMode: BlendMode) {
        if (index >= 0 && !bitmap.isRecycled) {
            val canvas = drawContext.canvas.nativeCanvas
            val left = index % col * width
            val top = index / col * height

            val oldAlpha = paint.alpha
            val oldFilter = paint.colorFilter
            val oldBlendMode = paint.blendMode
            paint.alpha = (alpha * 255).toInt()
            paint.colorFilter = filter?.asAndroidColorFilter()
            paint.blendMode = blendMode.asAndroidBlendMode()

            canvas.drawBitmap(
                bitmap,
                android.graphics.Rect(left, top, left + width, top + height),
                dst.toAndroidRectF(),
                paint
            )

            paint.alpha = oldAlpha
            paint.colorFilter = oldFilter
            paint.blendMode = oldBlendMode
        }
    }

    actual fun DrawScope.drawFrame(index: Int, position: Offset, size: Size, alpha: Float, filter: ColorFilter?, blendMode: BlendMode) {
        this.drawFrame(index, Rect(position, size), alpha, filter, blendMode)
    }

    actual fun encode(format: ImageFormat, quality: ImageQuality): ByteArray? = PlatformImage(bitmap).encode(format, quality)

    actual companion object {
        init {
            System.loadLibrary("webp_jni")
        }

        actual fun decode(data: ByteArray): AnimatedWebp? = catchingNull {
            var handle = 0L
            catchingThrow(
                clean = { nativeAnimatedWebpRelease(handle) }
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
                val buffer = ByteBuffer.allocateDirect(width * height * 4)
                val cacheBitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)

                var frame = 0

                while (nativeAnimatedWebpHasMoreFrames(handle)) {
                    buffer.rewind()
                    require(nativeAnimatedWebpGetNext(handle, buffer))
                    buffer.rewind()
                    cacheBitmap.copyPixelsFromBuffer(buffer)

                    val frameRow = frame / col
                    val frameCol = frame % col
                    val left = frameCol * width
                    val top = frameRow * height

                    canvas.drawBitmap(
                        cacheBitmap,
                        null,
                        android.graphics.Rect(left, top, left + width, top + height),
                        paint
                    )

                    ++frame
                }
                nativeAnimatedWebpReset(handle)
                require(frame == frameCount)

                mergeBitmap.isMutable
                AnimatedWebp(width, height, frameCount, row, col, paint, mergeBitmap)
            }
        }
    }
}