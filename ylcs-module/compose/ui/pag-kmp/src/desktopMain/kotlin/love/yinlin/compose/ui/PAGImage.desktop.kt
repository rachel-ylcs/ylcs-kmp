package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.graphics.asComposeMatrix
import love.yinlin.compose.graphics.asSkiaMatrix33
import org.jetbrains.skia.Matrix33

@Stable
actual class PAGImage(internal val delegate: PlatformPAGImage) {
    actual companion object {
        actual fun loadFromPath(path: String): PAGImage = PAGImage(PlatformPAGImage.loadFromPath(path))

        actual fun loadFromBytes(bytes: ByteArray): PAGImage = PAGImage(PlatformPAGImage.loadFromBytes(bytes))

        actual fun loadFromPixels(pixels: IntArray, width: Int, height: Int, rowBytes: Long, colorType: PAGColorType, alphaType: PAGAlphaType): PAGImage =
            PAGImage(PlatformPAGImage.loadFromPixels(pixels, width, height, rowBytes, colorType.ordinal, alphaType.ordinal))
    }

    actual val width: Int get() = delegate.width
    actual val height: Int get() = delegate.height
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode]
        set(value) { delegate.scaleMode = value.ordinal }
    actual var matrix: Matrix get() = Matrix33(*delegate.matrix).asComposeMatrix()
        set(value) { delegate.matrix = value.asSkiaMatrix33().mat }

    actual fun close() = delegate.close()
}