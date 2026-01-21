package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
expect object PAGVideoDecoder {
    fun setMaxHardwareDecoderCount(maxDecoderCount: Int)
}