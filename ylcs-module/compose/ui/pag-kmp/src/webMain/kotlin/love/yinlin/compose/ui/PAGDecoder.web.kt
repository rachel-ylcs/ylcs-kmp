package love.yinlin.compose.ui

import love.yinlin.platform.unsupportedPlatform

actual class PAGDecoder {
    actual companion object {
        actual fun makeFrom(composition: PAGComposition, maxFrameRate: Float, scale: Float): PAGDecoder = unsupportedPlatform()
    }

    actual val width: Int get() = unsupportedPlatform()
    actual val height: Int get() = unsupportedPlatform()
    actual val numFrames: Int get() = unsupportedPlatform()
    actual val frameRate: Float get() = unsupportedPlatform()
    actual fun checkFrameRate(index: Int): Boolean = unsupportedPlatform()
    actual fun close() { unsupportedPlatform() }
}