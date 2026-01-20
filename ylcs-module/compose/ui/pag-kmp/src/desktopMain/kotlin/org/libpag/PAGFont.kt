package org.libpag

import love.yinlin.annotation.NativeLibApi
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
data class PAGFont(val fontFamily: String = "", val fontStyle: String = "") {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeRegisterFontFromPath(path: String, ttcIndex: Int, fontFamily: String, fontStyle: String): Array<String>?
        @JvmStatic
        private external fun nativeRegisterFontFromBytes(bytes: ByteArray, ttcIndex: Int, fontFamily: String, fontStyle: String): Array<String>?
        @JvmStatic
        private external fun nativeUnregisterFont(fontFamily: String, fontStyle: String)
        @JvmStatic
        private external fun nativeSetFallbackFontNames(fontNameList: Array<String>)
        @JvmStatic
        private external fun nativeSetFallbackFontPaths(pathList: Array<String>, ttcIndices: IntArray)

        fun registerFont(path: String, ttcIndex: Int = 0, font: PAGFont = PAGFont()): PAGFont? {
            val result = nativeRegisterFontFromPath(path, ttcIndex, font.fontFamily, font.fontStyle)
            return if (result != null) PAGFont(result[0], result[1]) else null
        }
        fun registerFont(bytes: ByteArray, ttcIndex: Int = 0, font: PAGFont = PAGFont()): PAGFont? {
            val result = nativeRegisterFontFromBytes(bytes, ttcIndex, font.fontFamily, font.fontStyle)
            return if (result != null) PAGFont(result[0], result[1]) else null
        }
        fun unregisterFont(font: PAGFont) = nativeUnregisterFont(font.fontFamily, font.fontStyle)
        fun setFallbackFont(fontNameList: Array<String>) = nativeSetFallbackFontNames(fontNameList)
        fun setFallbackFont(pathList: Array<String>, ttcIndices: IntArray) = nativeSetFallbackFontPaths(pathList, ttcIndices)
    }
}