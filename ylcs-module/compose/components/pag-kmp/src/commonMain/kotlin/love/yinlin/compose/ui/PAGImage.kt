package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Matrix

@Stable
expect class PAGImage {
    companion object {
        fun loadFromPath(path: String): PAGImage
        fun loadFromBytes(bytes: ByteArray): PAGImage
        fun loadFromPixels(pixels: IntArray, width: Int, height: Int, rowBytes: Long, colorType: PAGColorType, alphaType: PAGAlphaType): PAGImage
    }

    val width: Int
    val height: Int
    var scaleMode: PAGScaleMode
    var matrix: Matrix

    fun close()
}