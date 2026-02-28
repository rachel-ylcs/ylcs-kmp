package org.libpag

import love.yinlin.annotation.NativeLibApi
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
class PAGSolidLayer internal constructor(
    constructor: () -> Long
) : PAGLayer(PAGSolidLayer::nativeRelease, constructor) {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeMake(duration: Long, width: Int, height: Int, solidColor: Int, opacity: Int): Long
        @JvmStatic
        private external fun nativeRelease(handle: Long)
        @JvmStatic
        private external fun nativeSolidColor(handle: Long): Int
        @JvmStatic
        private external fun nativeSetSolidColor(handle: Long, solidColor: Int)

        fun make(duration: Long, width: Int, height: Int, solidColor: Int, opacity: Int = 255): PAGSolidLayer {
            return PAGSolidLayer { nativeMake(duration, width, height, solidColor, opacity) }
        }
    }

    var solidColor: Int get() = nativeSolidColor(nativeHandle)
        set(value) = nativeSetSolidColor(nativeHandle, value)
}