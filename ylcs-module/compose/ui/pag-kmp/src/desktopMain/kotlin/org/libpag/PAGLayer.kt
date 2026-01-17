package org.libpag

import love.yinlin.extension.Destructible
import love.yinlin.extension.NativeLib
import love.yinlin.extension.RAII
import love.yinlin.platform.NativeLibLoader
import org.jetbrains.skia.Matrix33

@NativeLib
class PAGLayer private constructor(constructor: () -> Long) : Destructible(RAII(constructor, ::nativeRelease)) {
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
        private external fun nativeGetMatrix(handle: Long, arr: FloatArray)
        @JvmStatic
        private external fun nativeSetMatrix(handle: Long, arr: FloatArray)
        // parent()
        @JvmStatic
        private external fun nativeResetMatrix(handle: Long)
        @JvmStatic
        private external fun nativeGetTotalMatrix(handle: Long, arr: FloatArray)
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
    }

    val layerType: Int get() = nativeLayerType(nativeHandle)

    val layerName: String get() = nativeLayerName(nativeHandle)

    var matrix: Matrix33
        get() {
            val values = FloatArray(9)
            nativeGetMatrix(nativeHandle, values)
            return Matrix33(*values)
        }
        set(value) {
            nativeSetMatrix(nativeHandle, value.mat)
        }

    fun resetMatrix() { nativeResetMatrix(nativeHandle) }

    val totalMatrix: Matrix33 get() {
        val values = FloatArray(9)
        nativeGetTotalMatrix(nativeHandle, values)
        return Matrix33(*values)
    }

    var visible: Boolean get() = nativeVisible(nativeHandle)
        set(value) { nativeSetVisible(nativeHandle, value) }

    val editableIndex: Int get() = nativeEditableIndex(nativeHandle)

    fun localTimeToGlobal(time: Long): Long = nativeLocalTimeToGlobal(nativeHandle, time)

    fun globalToLocalTime(time: Long): Long = nativeGlobalToLocalTime(nativeHandle, time)

    val duration: Long get() = nativeDuration(nativeHandle)

    val frameRate: Float get() = nativeFrameRate(nativeHandle)
}