@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

actual object PAGVideoDecoder {
    actual fun setMaxHardwareDecoderCount(maxDecoderCount: Int) { PlatformPAGVideoDecoder.SetMaxHardwareDecoderCount(maxDecoderCount) }
}