package org.libpag

import love.yinlin.annotation.NativeLibApi
import love.yinlin.extension.Destructible
import love.yinlin.extension.RAII
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
class PAGDecoder private constructor(constructor: () -> Long) : Destructible(RAII(constructor, PAGDecoder::nativeRelease)), AutoCloseable {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeMakeFrom(compositionHandle: Long, maxFrameRate: Float, scale: Float): Long
        @JvmStatic
        private external fun nativeClear(handle: Long)
        @JvmStatic
        private external fun nativeRelease(handle: Long)
        @JvmStatic
        private external fun nativeWidth(handle: Long): Int
        @JvmStatic
        private external fun nativeHeight(handle: Long): Int
        @JvmStatic
        private external fun nativeNumFrames(handle: Long): Int
        @JvmStatic
        private external fun nativeFrameRate(handle: Long): Float
        @JvmStatic
        private external fun nativeCheckFrameChanged(handle: Long, index: Int): Boolean
        @JvmStatic
        private external fun nativeReadFrame(handle: Long, index: Int, colorType: Int, alphaType: Int, rowBytes: Long, container: ByteArray): Boolean

        fun makeFrom(composition: PAGComposition, maxFrameRate: Float = 30f, scale: Float = 1f) =
            PAGDecoder { nativeMakeFrom(composition.nativeHandle, maxFrameRate, scale) }
    }

    val width: Int get() = nativeWidth(nativeHandle)

    val height: Int get() = nativeHeight(nativeHandle)

    val numFrames: Int get() = nativeNumFrames(nativeHandle)

    val frameRate: Float get() = nativeFrameRate(nativeHandle)

    fun checkFrameRate(index: Int) = nativeCheckFrameChanged(nativeHandle, index)

    fun readFrame(index: Int, colorType: Int, alphaType: Int, rowBytes: Long, container: ByteArray) =
        nativeReadFrame(nativeHandle, index, colorType, alphaType, rowBytes, container)

    override fun close() {
        nativeClear(nativeHandle)
    }
}