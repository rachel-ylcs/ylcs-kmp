package org.libpag

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader

@NativeLib
class PAGSolidLayer internal constructor(constructor: () -> Long) : PAGLayer(constructor, PAGSolidLayer::nativeRelease) {
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

    var solidColor: Color get() = Color(nativeSolidColor(nativeHandle))
        set(value) = nativeSetSolidColor(nativeHandle, value.toArgb())
}