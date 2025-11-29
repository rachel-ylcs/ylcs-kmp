package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toSkiaRect
import love.yinlin.data.MimeType
import love.yinlin.data.compose.ImageFormat
import love.yinlin.data.compose.ImageQuality
import love.yinlin.extension.catchingNull
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

@Stable
actual class AnimatedWebp internal constructor(
    actual val width: Int,
    actual val height: Int,
    actual val frameCount: Int,
    private val row: Int,
    private val col: Int,
    private val paint: Paint,
    private val image: Image,
) {
    actual fun DrawScope.drawFrame(index: Int, dst: Rect) {
        if (index >= 0 && !image.isClosed) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawImageRect(
                    image = image,
                    src = org.jetbrains.skia.Rect.makeXYWH(
                        l = (index % col * width).toFloat(),
                        t = (index / col * height).toFloat(),
                        w = width.toFloat(),
                        h = height.toFloat()
                    ),
                    dst = dst.toSkiaRect(),
                    paint = paint,
                    samplingMode = SamplingMode.MITCHELL,
                    strict = true
                )
            }
        }
    }

    actual fun DrawScope.drawFrame(index: Int, position: Offset, size: Size) {
        this.drawFrame(index, Rect(position, size))
    }

    actual fun encode(format: ImageFormat, quality: ImageQuality): ByteArray? = PlatformImage(image).encode(format, quality)

    actual companion object {
        actual fun decode(data: ByteArray): AnimatedWebp? = catchingNull {
            require(checkHeader(data))
            Data.makeFromBytes(data).use { rawData ->
                Codec.makeFromData(rawData).use { codec ->
                    val info = codec.imageInfo
                    val width = info.width
                    val height = info.height
                    val frameCount = codec.frameCount
                    val mimeType = "image/${codec.encodedImageFormat.name.lowercase()}"
                    require(width > 0 && height > 0 && frameCount > 1 && mimeType == MimeType.WEBP)

                    val (row, col) = calculateGrid(width, height, frameCount)
                    require(row > 0 && col > 0)

                    val mergeWidth = col * width
                    val mergeHeight = row * height
                    val mergeBitmap = Bitmap()
                    mergeBitmap.allocPixels(ImageInfo(info.colorInfo, mergeWidth, mergeHeight))

                    val paint = Paint()
                    paint.isAntiAlias = true

                    Canvas(mergeBitmap).use { canvas ->
                        Bitmap().use { cacheBitmap ->
                            cacheBitmap.allocPixels(ImageInfo(info.colorInfo, width, height))
                            cacheBitmap.setImmutable()
                            outer@ for (i in 0 ..< row) {
                                for (j in 0 ..< col) {
                                    val frame = i * col + j
                                    if (frame >= frameCount) break@outer

                                    codec.readPixels(cacheBitmap, frame)
                                    cacheBitmap.notifyPixelsChanged()
                                    Image.makeFromBitmap(cacheBitmap).use { image ->
                                        canvas.drawImageRect(
                                            image = image,
                                            src = org.jetbrains.skia.Rect.makeWH(
                                                w = width.toFloat(),
                                                h = height.toFloat()
                                            ),
                                            dst = org.jetbrains.skia.Rect.makeXYWH(
                                                l = (j * width).toFloat(),
                                                t = (i * height).toFloat(),
                                                w = width.toFloat(),
                                                h = height.toFloat()
                                            ),
                                            samplingMode = SamplingMode.MITCHELL,
                                            paint = paint,
                                            strict = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    mergeBitmap.setImmutable()
                    AnimatedWebp(width, height, frameCount, row, col, paint, Image.makeFromBitmap(mergeBitmap))
                }
            }
        }
    }
}