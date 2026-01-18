package org.libpag

import love.yinlin.compose.ui.PAGLayerType
import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader

@NativeLib
class PAGFile internal constructor(constructor: () -> Long) : PAGLayer(constructor, PAGFile::nativeRelease) {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeMaxSupportedTagLevel(): Short
        @JvmStatic
        private external fun nativeLoadFromPath(path: String): Long
        @JvmStatic
        private external fun nativeLoadFromBytes(bytes: ByteArray): Long
        @JvmStatic
        private external fun nativeRelease(handle: Long)
        @JvmStatic
        private external fun nativeTagLevel(handle: Long): Int
        @JvmStatic
        private external fun nativeNumTexts(handle: Long): Int
        @JvmStatic
        private external fun nativeNumImages(handle: Long): Int
        @JvmStatic
        private external fun nativeNumVideos(handle: Long): Int
        @JvmStatic
        private external fun nativePath(handle: Long): String
        // getTextData()
        // replaceText()
        @JvmStatic
        private external fun nativeReplaceImage(handle: Long, index: Int, imageHandle: Long)
        @JvmStatic
        private external fun nativeReplaceImageByName(handle: Long, layerName: String, imageHandle: Long)
        // getLayersByEditableIndex()
        @JvmStatic
        private external fun nativeGetEditableIndices(handle: Long, layerType: Int): IntArray
        @JvmStatic
        private external fun nativeTimeStretchMode(handle: Long): Int
        @JvmStatic
        private external fun nativeSetTimeStretchMode(handle: Long, mode: Int)
        @JvmStatic
        private external fun nativeSetDuration(handle: Long, duration: Long)
        @JvmStatic
        private external fun nativeCopyOriginal(handle: Long): Long

        val MaxSupportedTagLevel: Short get() = nativeMaxSupportedTagLevel()

        fun loadFromPath(path: String): PAGFile = PAGFile { nativeLoadFromPath(path) }
        
        fun loadFromBytes(bytes: ByteArray): PAGFile = PAGFile { nativeLoadFromBytes(bytes) }
    }

    val tagLevel: Int get() = nativeTagLevel(nativeHandle)

    val numTexts: Int get() = nativeNumTexts(nativeHandle)

    val numImages: Int get() = nativeNumImages(nativeHandle)

    val numVideos: Int get() = nativeNumVideos(nativeHandle)

    val path: String get() = nativePath(nativeHandle)

    fun replaceImage(index: Int, image: PAGImage) = nativeReplaceImage(nativeHandle, index, image.nativeHandle)

    fun replaceImageByName(layerName: String, image: PAGImage) = nativeReplaceImageByName(nativeHandle, layerName, image.nativeHandle)

    fun getEditableIndices(layerType: PAGLayerType): IntArray = nativeGetEditableIndices(nativeHandle, layerType.originType)

    var timeStretchMode: Int get() = nativeTimeStretchMode(nativeHandle)
        set(value) { nativeSetTimeStretchMode(nativeHandle, value) }

    fun setDuration(duration: Long) = nativeSetDuration(nativeHandle, duration)

    fun copyOriginal(): PAGFile = PAGFile { nativeCopyOriginal(nativeHandle) }
}