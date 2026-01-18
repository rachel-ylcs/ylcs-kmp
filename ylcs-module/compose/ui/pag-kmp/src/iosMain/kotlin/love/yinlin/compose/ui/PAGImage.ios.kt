@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Matrix
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.compose.graphics.asCGAffineTransform
import love.yinlin.compose.graphics.asComposeMatrix

actual class PAGImage(private val delegate: PlatformPAGImage) {
    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode()]
        set(value) { delegate.setScaleMode(value.ordinal) }
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asCGAffineTransform()) }

    actual fun close() { }

    actual companion object {
        actual fun loadFromPath(path: String): PAGImage = PAGImage(PlatformPAGImage.FromPath(path))
        actual fun loadFromBytes(bytes: ByteArray): PAGImage = PAGImage(PlatformPAGImage.FromBytes(bytes, bytes.size))
        actual fun loadFromPixels(pixels: IntArray, width: Int, height: Int, rowBytes: Long, colorType: PAGColorType, alphaType: PAGAlphaType): PAGImage =
            PAGImage(PlatformPAGImage.FromPixelBuffer(pixels, width, height, rowBytes, colorType.ordinal, alphaType.ordinal))
    }
}