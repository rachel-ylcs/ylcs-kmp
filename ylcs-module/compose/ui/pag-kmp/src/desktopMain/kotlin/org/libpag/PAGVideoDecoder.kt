package org.libpag

import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader

@NativeLib
object PAGVideoDecoder {
    init {
        NativeLibLoader.resource("pag_kmp")
    }

    @JvmStatic
    private external fun nativeSetMaxHardwareDecoderCount(maxCount: Int)

    fun setMaxHardwareDecoderCount(maxDecoderCount: Int) = nativeSetMaxHardwareDecoderCount(maxDecoderCount)
}