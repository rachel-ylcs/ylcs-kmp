package org.libpag

import love.yinlin.annotation.NativeLibApi
import love.yinlin.extension.Destructible
import love.yinlin.extension.RAII
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
class PAGSurface private constructor(constructor: () -> Long) : Destructible(RAII(constructor, PAGSurface::nativeRelease)), AutoCloseable {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeMakeOffscreen(width: Int, height: Int): Long
        @JvmStatic
        private external fun nativeClear(handle: Long)
        @JvmStatic
        private external fun nativeRelease(handle: Long)
        @JvmStatic
        private external fun nativeWidth(handle: Long): Int
        @JvmStatic
        private external fun nativeHeight(handle: Long): Int
        @JvmStatic
        private external fun nativeUpdateSize(handle: Long)
        @JvmStatic
        private external fun nativeClearAll(handle: Long)
        @JvmStatic
        private external fun nativeFreeCache(handle: Long)
        @JvmStatic
        private external fun nativeReadPixels(handle: Long, colorType: Int, alphaType: Int, rowBytes: Long, container: IntArray): Boolean

        fun makeOffscreen(width: Int, height: Int): PAGSurface = PAGSurface { nativeMakeOffscreen(width, height) }
    }

    val width: Int get() = nativeWidth(nativeHandle)

    val height: Int get() = nativeHeight(nativeHandle)

    fun updateSize() = nativeUpdateSize(nativeHandle)

    fun clearAll() = nativeClearAll(nativeHandle)

    fun readPixels(colorType: Int, alphaType: Int, rowBytes: Long, container: IntArray) =
        nativeReadPixels(nativeHandle, colorType, alphaType, rowBytes, container)

    override fun close() {
        // Must call freeCache() here, otherwise, the cache may not be freed until the PAGPlayer is
        // garbage collected.
        nativeFreeCache(nativeHandle)
        nativeClear(nativeHandle)
    }
}