package org.libpag

import love.yinlin.extension.Destructible
import love.yinlin.extension.NativeLib
import love.yinlin.extension.RAII
import love.yinlin.platform.NativeLibLoader
import org.jetbrains.skia.Matrix33

@NativeLib
class PAGImage private constructor(constructor: () -> Long) : Destructible(RAII(constructor, ::nativeRelease)), AutoCloseable {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeLoadFromPath(path: String): Long
        @JvmStatic
        private external fun nativeLoadFromBytes(bytes: ByteArray): Long
        @JvmStatic
        private external fun nativeLoadFromPixels(pixels: ByteArray, width: Int, height: Int, rowBytes: Long, colorType: Int, alphaType: Int): Long
        @JvmStatic
        private external fun nativeClear(handle: Long)
        @JvmStatic
        private external fun nativeRelease(handle: Long)
        @JvmStatic
        private external fun nativeWidth(handle: Long): Int
        @JvmStatic
        private external fun nativeHeight(handle: Long): Int
        @JvmStatic
        private external fun nativeScaleMode(handle: Long): Int
        @JvmStatic
        private external fun nativeSetScaleMode(handle: Long, value: Int)
        @JvmStatic
        private external fun nativeGetMatrix(handle: Long, arr: FloatArray)
        @JvmStatic
        private external fun nativeSetMatrix(handle: Long, arr: FloatArray)

        fun loadFromPath(path: String): PAGImage = PAGImage { nativeLoadFromPath(path) }
        fun loadFromBytes(bytes: ByteArray): PAGImage = PAGImage { nativeLoadFromBytes(bytes) }
        fun loadFromPixels(pixels: ByteArray, width: Int, height: Int, rowBytes: Long, colorType: Int, alphaType: Int): PAGImage =
            PAGImage { nativeLoadFromPixels(pixels, width, height, rowBytes, colorType, alphaType) }
    }

    val width: Int get() = nativeWidth(nativeHandle)

    val height: Int get() = nativeHeight(nativeHandle)

    var scaleMode: Int get() = nativeScaleMode(nativeHandle)
        set(value) { nativeSetScaleMode(nativeHandle, value) }

    var matrix: Matrix33
        get() {
            val values = FloatArray(9)
            nativeGetMatrix(nativeHandle, values)
            return Matrix33(*values)
        }
        set(value) {
            nativeSetMatrix(nativeHandle, value.mat)
        }

    override fun close() = nativeClear(nativeHandle)
}