package org.libpag

import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader

@NativeLib
data class PAGFont(val fontFamily: String = "", val fontStyle: String = "") {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeUnregisterFont(fontFamily: String, fontStyle: String)
        @JvmStatic
        private external fun nativeSetFallbackFontPaths(fontNameList: Array<String>, ttcIndices: IntArray)
        @JvmStatic
        private external fun nativeRegisterFont(bytes: ByteArray, ttcIndex: Int, fontFamily: String, fontStyle: String): Array<String>?

        fun unregisterFont(font: PAGFont) = nativeUnregisterFont(font.fontFamily, font.fontStyle)
        fun setFallbackFontPaths(fontNameList: Array<String>, ttcIndices: IntArray) = nativeSetFallbackFontPaths(fontNameList, ttcIndices)
        fun registerFont(bytes: ByteArray, ttcIndex: Int, font: PAGFont): PAGFont? {
            val result = nativeRegisterFont(bytes, ttcIndex, font.fontFamily, font.fontStyle)
            return if (result != null) PAGFont(result[0], result[1]) else null
        }
    }
}