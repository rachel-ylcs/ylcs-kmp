package org.libpag

import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader
import java.nio.ByteBuffer

@NativeLib
class PAGImageLayer internal constructor(constructor: () -> Long) : PAGLayer(constructor, PAGImageLayer::nativeRelease) {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeMake(width: Int, height: Int, duration: Long): Long
        @JvmStatic
        private external fun nativeRelease(handle: Long)
        @JvmStatic
        private external fun nativeContentDuration(handle: Long): Long
        // getVideoRanges()
        @JvmStatic
        private external fun nativeReplaceImage(handle: Long, imageHandle: Long)
        @JvmStatic
        private external fun nativeSetImage(handle: Long, imageHandle: Long)
        @JvmStatic
        private external fun nativeLayerTimeToContent(handle: Long, time: Long): Long
        @JvmStatic
        private external fun nativeContentTimeToLayer(handle: Long, time: Long): Long
        @JvmStatic
        private external fun nativeImageBytes(handle: Long): ByteBuffer

        fun make(width: Int, height: Int, duration: Long): PAGImageLayer {
            return PAGImageLayer { nativeMake(width, height, duration) }
        }
    }

    val contentDuration: Long get() = nativeContentDuration(nativeHandle)

    fun replaceImage(image: PAGImage) = nativeReplaceImage(nativeHandle, image.nativeHandle)

    fun setImage(image: PAGImage) = nativeSetImage(nativeHandle, image.nativeHandle)

    fun layerTimeToContent(time: Long): Long = nativeLayerTimeToContent(nativeHandle, time)

    fun contentTimeToLayer(time: Long): Long = nativeContentTimeToLayer(nativeHandle, time)

    val imageBytes: ByteBuffer get() = nativeImageBytes(nativeHandle)
}