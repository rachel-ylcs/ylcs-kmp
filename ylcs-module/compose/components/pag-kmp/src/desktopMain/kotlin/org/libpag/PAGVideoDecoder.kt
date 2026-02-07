package org.libpag

import love.yinlin.annotation.NativeLibApi
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
object PAGVideoDecoder {
    init {
        NativeLibLoader.resource("pag_kmp")
    }

    @JvmStatic
    private external fun nativeSetMaxHardwareDecoderCount(maxCount: Int)

    fun setMaxHardwareDecoderCount(maxDecoderCount: Int) = nativeSetMaxHardwareDecoderCount(maxDecoderCount)
}