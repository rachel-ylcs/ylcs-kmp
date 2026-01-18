package org.libpag

import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader

@NativeLib
class PAGTextLayer internal constructor(
    constructor: () -> Long
) : PAGLayer(PAGTextLayer::nativeRelease, constructor) {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeMake(duration: Long, text: String, fontSize: Float, fontFamily: String, fontStyle: String): Long
        @JvmStatic
        private external fun nativeRelease(handle: Long)
        @JvmStatic
        private external fun nativeFillColor(handle: Long): Int
        @JvmStatic
        private external fun nativeSetFillColor(handle: Long, color: Int)
        @JvmStatic
        private external fun nativeFont(handle: Long): Array<String>?
        @JvmStatic
        private external fun nativeSetFont(handle: Long, fontFamily: String, fontStyle: String)
        @JvmStatic
        private external fun nativeFontSize(handle: Long): Float
        @JvmStatic
        private external fun nativeSetFontSize(handle: Long, fontSize: Float)
        @JvmStatic
        private external fun nativeStrokeColor(handle: Long): Int
        @JvmStatic
        private external fun nativeSetStrokeColor(handle: Long, color: Int)
        @JvmStatic
        private external fun nativeText(handle: Long): String
        @JvmStatic
        private external fun nativeSetText(handle: Long, text: String)
        @JvmStatic
        private external fun nativeReset(handle: Long)

        fun make(duration: Long, text: String, fontSize: Float = 24f, font: PAGFont = PAGFont()): PAGTextLayer {
            return PAGTextLayer { nativeMake(duration, text, fontSize, font.fontFamily, font.fontStyle) }
        }
    }

    var fillColor: Int get() = nativeFillColor(nativeHandle)
        set(value) { nativeSetFillColor(nativeHandle, value) }

    var font: PAGFont
        get() {
            val result = nativeFont(nativeHandle)
            return if (result != null) PAGFont(result[0], result[1]) else PAGFont()
        }
        set(value) {
            nativeSetFont(nativeHandle, value.fontFamily, value.fontStyle)
        }

    var fontSize: Float get() = nativeFontSize(nativeHandle)
        set(value) { nativeSetFontSize(nativeHandle, value) }

    var strokeColor: Int get() = nativeStrokeColor(nativeHandle)
        set(value) { nativeSetStrokeColor(nativeHandle, value) }

    var text: String get() = nativeText(nativeHandle)
        set(value) { nativeSetText(nativeHandle, value) }

    fun reset() = nativeReset(nativeHandle)
}