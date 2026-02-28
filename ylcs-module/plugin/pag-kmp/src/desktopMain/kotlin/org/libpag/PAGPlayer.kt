package org.libpag

import love.yinlin.annotation.NativeLibApi
import love.yinlin.extension.Destructible
import love.yinlin.extension.RAII
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
class PAGPlayer private constructor(constructor: () -> Long) : Destructible(RAII(constructor, PAGPlayer::nativeRelease)), AutoCloseable {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeCreate(): Long
        @JvmStatic
        private external fun nativeClear(handle: Long)
        @JvmStatic
        private external fun nativeRelease(handle: Long)
        @JvmStatic
        private external fun nativeSetSurface(handle: Long, surfaceHandle: Long)
        @JvmStatic
        private external fun nativeGetComposition(handle: Long): LongArray
        @JvmStatic
        private external fun nativeSetComposition(handle: Long, compositionHandle: Long, type: Long)
        @JvmStatic
        private external fun nativeVideoEnabled(handle: Long): Boolean
        @JvmStatic
        private external fun nativeSetVideoEnabled(handle: Long, value: Boolean)
        @JvmStatic
        private external fun nativeCacheEnabled(handle: Long): Boolean
        @JvmStatic
        private external fun nativeSetCacheEnabled(handle: Long, value: Boolean)
        @JvmStatic
        private external fun nativeUseDiskCache(handle: Long): Boolean
        @JvmStatic
        private external fun nativeSetUseDiskCache(handle: Long, value: Boolean)
        @JvmStatic
        private external fun nativeCacheScale(handle: Long): Float
        @JvmStatic
        private external fun nativeSetCacheScale(handle: Long, value: Float)
        @JvmStatic
        private external fun nativeMaxFrameRate(handle: Long): Float
        @JvmStatic
        private external fun nativeSetMaxFrameRate(handle: Long, value: Float)
        @JvmStatic
        private external fun nativeScaleMode(handle: Long): Int
        @JvmStatic
        private external fun nativeSetScaleMode(handle: Long, value: Int)
        @JvmStatic
        private external fun nativeGetMatrix(handle: Long): FloatArray
        @JvmStatic
        private external fun nativeSetMatrix(handle: Long, value: FloatArray)
        @JvmStatic
        private external fun nativeDuration(handle: Long): Long
        @JvmStatic
        private external fun nativeGetProgress(handle: Long): Double
        @JvmStatic
        private external fun nativeSetProgress(handle: Long, value: Double)
        @JvmStatic
        private external fun nativeCurrentFrame(handle: Long): Long
        @JvmStatic
        private external fun nativePrepare(handle: Long)
        @JvmStatic
        private external fun nativeFlush(handle: Long)
        @JvmStatic
        private external fun nativeFlushAndFenceSync(handle: Long, syncArray: LongArray): Boolean
        @JvmStatic
        private external fun nativeWaitSync(handle: Long, sync: Long): Boolean
        // getBounds()
        // getLayersUnderPoint()
        @JvmStatic
        private external fun nativeHitTestPoint(handle: Long, layerHandle: Long, type: Long, x: Float, y: Float, pixelHitTest: Boolean): Boolean
        @JvmStatic
        private external fun nativeRenderingTime(handle: Long): Long
        @JvmStatic
        private external fun nativeImageDecodingTime(handle: Long): Long
        @JvmStatic
        private external fun nativePresentingTime(handle: Long): Long
        @JvmStatic
        private external fun nativeGraphicsMemory(handle: Long): Long
    }

    constructor() : this(::nativeCreate)

    var surface: PAGSurface? = null
        set(value) {
            field = value
            if (value == null) nativeSetSurface(nativeHandle, 0L)
            else nativeSetSurface(nativeHandle, value.nativeHandle)
        }

    var composition: PAGComposition? get() = unpackLayerInfo(nativeGetComposition(nativeHandle)) as? PAGComposition
        set(value) {
            value?.let { nativeSetComposition(nativeHandle, it.nativeHandle, it.internalLayerType) }
        }

    var videoEnabled: Boolean get() = nativeVideoEnabled(nativeHandle)
        set(value) { nativeSetVideoEnabled(nativeHandle, value) }

    var cacheEnabled: Boolean get() = nativeCacheEnabled(nativeHandle)
        set(value) { nativeSetCacheEnabled(nativeHandle, value) }

    var useDiskCache: Boolean get() = nativeUseDiskCache(nativeHandle)
        set(value) { nativeSetUseDiskCache(nativeHandle, value) }

    var cacheScale: Float get() = nativeCacheScale(nativeHandle)
        set(value) { nativeSetCacheScale(nativeHandle, value) }

    var maxFrameRate: Float get() = nativeMaxFrameRate(nativeHandle)
        set(value) { nativeSetMaxFrameRate(nativeHandle, value) }

    var scaleMode: Int get() = nativeScaleMode(nativeHandle)
        set(value) { nativeSetScaleMode(nativeHandle, value) }

    var matrix: FloatArray get() = nativeGetMatrix(nativeHandle)
        set(value) { nativeSetMatrix(nativeHandle, value) }

    val duration: Long get() = nativeDuration(nativeHandle)

    var progress: Double get() = nativeGetProgress(nativeHandle)
        set(value) { nativeSetProgress(nativeHandle, value) }

    val currentFrame: Long get() = nativeCurrentFrame(nativeHandle)

    fun prepare() = nativePrepare(nativeHandle)

    fun flush() = nativeFlush(nativeHandle)

    fun flushAndFenceSync(syncArray: LongArray) = nativeFlushAndFenceSync(nativeHandle, syncArray)

    fun waitSync(sync: Long) = nativeWaitSync(nativeHandle, sync)

    fun hitTestPoint(layer: PAGLayer, x: Float, y: Float, pixelHitTest: Boolean) = nativeHitTestPoint(nativeHandle, layer.nativeHandle, layer.internalLayerType, x, y, pixelHitTest)

    val renderingTime: Long get() = nativeRenderingTime(nativeHandle)

    val imageDecodingTime: Long get() = nativeImageDecodingTime(nativeHandle)

    val presentingTime: Long get() = nativePresentingTime(nativeHandle)

    val graphicsMemory: Long get() = nativeGraphicsMemory(nativeHandle)

    override fun close() {
        nativeClear(nativeHandle)
    }
}