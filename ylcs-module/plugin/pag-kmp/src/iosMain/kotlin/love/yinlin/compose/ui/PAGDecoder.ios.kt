@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import kotlinx.cinterop.ExperimentalForeignApi

@Stable
actual class PAGDecoder(private val delegate: PlatformPAGDecoder) {
    actual companion object {
        actual fun makeFrom(composition: PAGComposition, maxFrameRate: Float, scale: Float): PAGDecoder =
            PAGDecoder(PlatformPAGDecoder.Make(composition.delegate, maxFrameRate, scale)!!)
    }

    actual val width: Int get() = delegate.width().toInt()
    actual val height: Int get() = delegate.height().toInt()
    actual val numFrames: Int get() = delegate.numFrames().toInt()
    actual val frameRate: Float get() = delegate.frameRate()
    actual fun checkFrameRate(index: Int): Boolean = delegate.checkFrameChanged(index)
    actual fun close() { }
}