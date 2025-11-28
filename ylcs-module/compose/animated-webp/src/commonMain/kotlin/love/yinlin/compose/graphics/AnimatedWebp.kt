package love.yinlin.compose.graphics

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope

@Stable
expect class AnimatedWebp {
    val width: Int
    val height: Int
    val frameCount: Int

    suspend fun nextFrame()
    fun resetFrame()
    fun DrawScope.drawFrame(dst: Rect, src: Rect? = null)
    fun release()

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