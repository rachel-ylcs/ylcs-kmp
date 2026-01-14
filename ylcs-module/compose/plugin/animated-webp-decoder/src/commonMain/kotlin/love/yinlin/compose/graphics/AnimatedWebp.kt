package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import love.yinlin.compose.data.ImageFormat
import love.yinlin.compose.data.ImageQuality
import kotlin.math.*

@Stable
expect class AnimatedWebp {
    val width: Int
    val height: Int
    val frameCount: Int

    fun DrawScope.drawFrame(index: Int, dst: Rect, alpha: Float = 1f, filter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver)
    fun DrawScope.drawFrame(index: Int, position: Offset = Offset.Zero, size: Size = Size(width.toFloat(), height.toFloat()), alpha: Float = 1f, filter: ColorFilter? = null, blendMode: BlendMode = BlendMode.SrcOver)
    fun encode(format: ImageFormat = ImageFormat.WEBP, quality: ImageQuality = ImageQuality.Full): ByteArray?

    companion object {
        fun decode(data: ByteArray): AnimatedWebp?
    }
}

internal fun AnimatedWebp.Companion.checkHeader(data: ByteArray): Boolean {
    if (data.size <= 34) return false
    // RIFF
    if (data[0] != 0x52.toByte() || data[1] != 0x49.toByte() || data[2] != 0x46.toByte() || data[3] != 0x46.toByte()) return false
    // WEBP
    if (data[8] != 0x57.toByte() || data[9] != 0x45.toByte() || data[10] != 0x42.toByte() || data[11] != 0x50.toByte()) return false
    // VP8X
    if (data[12] != 0x56.toByte() || data[13] != 0x50.toByte() || data[14] != 0x38.toByte() || data[15] != 0x58.toByte()) return false
    // ChunkSize
    if ((data[16].toInt() and 0x02) == 0) return false
    // ANIM
    if (data[30] != 0x41.toByte() || data[31] != 0x4E.toByte() || data[32] != 0x49.toByte() || data[33] != 0x4D.toByte()) return false
    return true
}

internal fun AnimatedWebp.Companion.calculateGrid(width: Int, height: Int, frameCount: Int): Pair<Int, Int> {
    val startCols = sqrt(frameCount.toDouble()).toInt()
    var bestRows = (frameCount + startCols - 1) / startCols
    var bestCols = startCols
    var minAspectRatioDiff = Float.MAX_VALUE
    for (cols in max(1, startCols - 5)..min(frameCount, startCols + 5)) {
        val rows = (frameCount + cols - 1) / cols
        if (rows <= 0) continue
        val totalWidth = cols * width
        val totalHeight = rows * height
        val aspectRatio = if (totalWidth > totalHeight) totalWidth.toFloat() / totalHeight else totalHeight.toFloat() / totalWidth
        val aspectRatioDiff = abs(aspectRatio - 1.0f)
        if (aspectRatioDiff < minAspectRatioDiff) {
            minAspectRatioDiff = aspectRatioDiff
            bestCols = cols
            bestRows = rows
        }
    }
    return bestRows to bestCols
}