package org.libpag

import love.yinlin.extension.Destructible
import love.yinlin.extension.NativeLib
import love.yinlin.extension.RAII
import love.yinlin.platform.NativeLibLoader

@NativeLib
open class PAGLayer internal constructor(
    destructor: (Long) -> Unit = PAGLayer::nativeRelease,
    constructor: () -> Long,
) : Destructible(RAII(constructor, destructor)) {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeRelease(handle: Long)
        @JvmStatic
        private external fun nativeLayerType(handle: Long): Int
        @JvmStatic
        private external fun nativeLayerName(handle: Long): String
        @JvmStatic
        private external fun nativeGetMatrix(handle: Long): FloatArray
        @JvmStatic
        private external fun nativeSetMatrix(handle: Long, arr: FloatArray)
        // parent()
        @JvmStatic
        private external fun nativeResetMatrix(handle: Long)
        @JvmStatic
        private external fun nativeGetTotalMatrix(handle: Long): FloatArray
        @JvmStatic
        private external fun nativeVisible(handle: Long): Boolean
        @JvmStatic
        private external fun nativeSetVisible(handle: Long, value: Boolean)
        @JvmStatic
        private external fun nativeEditableIndex(handle: Long): Int
        // markers()
        @JvmStatic
        private external fun nativeLocalTimeToGlobal(handle: Long, time: Long): Long
        @JvmStatic
        private external fun nativeGlobalToLocalTime(handle: Long, time: Long): Long
        @JvmStatic
        private external fun nativeDuration(handle: Long): Long
        @JvmStatic
        private external fun nativeFrameRate(handle: Long): Float
        @JvmStatic
        private external fun nativeStartTime(handle: Long): Long
        @JvmStatic
        private external fun nativeSetStartTime(handle: Long, time: Long)
        @JvmStatic
        private external fun nativeCurrentTime(handle: Long): Long
        @JvmStatic
        private external fun nativeSetCurrentTime(handle: Long, time: Long)
        @JvmStatic
        private external fun nativeGetProgress(handle: Long): Double
        @JvmStatic
        private external fun nativeSetProgress(handle: Long, progress: Double)
        // trackMatteLayer()
        @JvmStatic
        private external fun nativeGetBounds(handle: Long): FloatArray
        @JvmStatic
        private external fun nativeExcludedFromTimeline(handle: Long): Boolean
        @JvmStatic
        private external fun nativeSetExcludedFromTimeline(handle: Long, value: Boolean)
        @JvmStatic
        private external fun nativeAlpha(handle: Long): Float
        @JvmStatic
        private external fun nativeSetAlpha(handle: Long, alpha: Float)
    }

    internal val internalLayerType: Long get() = when (this) {
        is PAGFile -> PAGLayerType.File
        is PAGComposition -> PAGLayerType.PreCompose
        is PAGSolidLayer -> PAGLayerType.Solid
        is PAGTextLayer -> PAGLayerType.Text
        is PAGShapeLayer -> PAGLayerType.Shape
        is PAGImageLayer -> PAGLayerType.Image
        else -> PAGLayerType.Unknown
    }.value

    val layerType: Int get() = nativeLayerType(nativeHandle)

    val layerName: String get() = nativeLayerName(nativeHandle)

    var matrix: FloatArray get() = nativeGetMatrix(nativeHandle)
        set(value) { nativeSetMatrix(nativeHandle, value) }

    fun resetMatrix() { nativeResetMatrix(nativeHandle) }

    val totalMatrix: FloatArray get() = nativeGetTotalMatrix(nativeHandle)

    var visible: Boolean get() = nativeVisible(nativeHandle)
        set(value) { nativeSetVisible(nativeHandle, value) }

    val editableIndex: Int get() = nativeEditableIndex(nativeHandle)

    fun localTimeToGlobal(time: Long): Long = nativeLocalTimeToGlobal(nativeHandle, time)

    fun globalToLocalTime(time: Long): Long = nativeGlobalToLocalTime(nativeHandle, time)

    val duration: Long get() = nativeDuration(nativeHandle)

    val frameRate: Float get() = nativeFrameRate(nativeHandle)

    var startTime: Long get() = nativeStartTime(nativeHandle)
        set(value) { nativeSetStartTime(nativeHandle, value) }

    var currentTime: Long get() = nativeCurrentTime(nativeHandle)
        set(value) { nativeSetCurrentTime(nativeHandle, value) }

    var progress: Double get() = nativeGetProgress(nativeHandle)
        set(value) { nativeSetProgress(nativeHandle, value) }

    val bounds: FloatArray get() = nativeGetBounds(nativeHandle)

    var excludedFromTimeline: Boolean get() = nativeExcludedFromTimeline(nativeHandle)
        set(value) { nativeSetExcludedFromTimeline(nativeHandle, value) }

    var alpha: Float get() = nativeAlpha(nativeHandle)
        set(value) { nativeSetAlpha(nativeHandle, value) }
}