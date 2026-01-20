@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Matrix
import love.yinlin.platform.unsupportedPlatform
import kotlin.js.ExperimentalWasmJsInterop

actual class PAGImage(internal val delegate: PlatformPAGImage) {
    actual companion object {
        actual fun loadFromPath(path: String): PAGImage = unsupportedPlatform()

        actual fun loadFromBytes(bytes: ByteArray): PAGImage = unsupportedPlatform()

        actual fun loadFromPixels(pixels: IntArray, width: Int, height: Int, rowBytes: Long, colorType: PAGColorType, alphaType: PAGAlphaType): PAGImage = unsupportedPlatform()
    }

    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode()]
        set(value) { delegate.setScaleMode(value.ordinal)  }
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asPAGMatrix()) }

    actual fun close() = delegate.destroy()
}