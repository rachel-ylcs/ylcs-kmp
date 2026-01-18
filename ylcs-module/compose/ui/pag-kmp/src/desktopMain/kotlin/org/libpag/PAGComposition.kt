package org.libpag

import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader

@NativeLib
open class PAGComposition internal constructor(
    destructor: (Long) -> Unit = PAGComposition::nativeRelease,
    constructor: () -> Long,
) : PAGLayer(destructor, constructor) {
    companion object {
        init {
            NativeLibLoader.resource("pag_kmp")
        }

        @JvmStatic
        private external fun nativeMake(width: Int, height: Int): Long
        @JvmStatic
        private external fun nativeRelease(handle: Long)
        @JvmStatic
        private external fun nativeWidth(handle: Long): Int
        @JvmStatic
        private external fun nativeHeight(handle: Long): Int
        @JvmStatic
        private external fun nativeSetContentSize(handle: Long, width: Int, height: Int)
        @JvmStatic
        private external fun nativeNumChildren(handle: Long): Int
        @JvmStatic
        private external fun nativeGetLayerAt(handle: Long, index: Int): LongArray
        @JvmStatic
        private external fun nativeGetLayerIndex(handle: Long, layerHandle: Long, type: Long): Int
        @JvmStatic
        private external fun nativeSetLayerIndex(handle: Long, layerHandle: Long, type: Long, index: Int): Int
        @JvmStatic
        private external fun nativeAddLayer(handle: Long, layerHandle: Long, type: Long)
        @JvmStatic
        private external fun nativeAddLayerAt(handle: Long, layerHandle: Long, type: Long, index: Int)
        @JvmStatic
        private external fun nativeContains(handle: Long, layerHandle: Long, type: Long): Boolean
        @JvmStatic
        private external fun nativeRemoveLayer(handle: Long, layerHandle: Long, type: Long): LongArray
        @JvmStatic
        private external fun nativeRemoveLayerAt(handle: Long, index: Int): LongArray
        @JvmStatic
        private external fun nativeRemoveAllLayers(handle: Long)
        @JvmStatic
        private external fun nativeSwapLayer(handle: Long, layerHandle1: Long, type1: Long, layerHandle2: Long, type2: Long)
        @JvmStatic
        private external fun nativeSwapLayerAt(handle: Long, index1: Int, index2: Int)
        @JvmStatic
        private external fun nativeAudioBytes(handle: Long): ByteArray?
        @JvmStatic
        private external fun nativeAudioStartTime(handle: Long): Long
        // audioMarkers()
        // getLayersByName()
        // getLayersUnderPoint()

        fun make(width: Int, height: Int): PAGComposition = PAGComposition { nativeMake(width, height) }
    }

    val width: Int get() = nativeWidth(nativeHandle)

    val height: Int get() = nativeHeight(nativeHandle)

    fun setContentSize(width: Int, height: Int) = nativeSetContentSize(nativeHandle, width, height)

    val numChildren: Int get() = nativeNumChildren(nativeHandle)

    fun getLayerAt(index: Int): PAGLayer? = unpackLayerInfo(nativeGetLayerAt(nativeHandle, index))

    fun getLayerIndex(layer: PAGLayer): Int = nativeGetLayerIndex(nativeHandle, layer.nativeHandle, layer.internalLayerType)

    fun setLayerIndex(layer: PAGLayer, index: Int) = nativeSetLayerIndex(nativeHandle, layer.nativeHandle, layer.internalLayerType, index)

    fun addLayer(layer: PAGLayer) = nativeAddLayer(nativeHandle, layer.nativeHandle, layer.internalLayerType)

    fun addLayerAt(layer: PAGLayer, index: Int) = nativeAddLayerAt(nativeHandle, layer.nativeHandle, layer.internalLayerType, index)

    operator fun contains(layer: PAGLayer): Boolean = nativeContains(nativeHandle, layer.nativeHandle, layer.internalLayerType)

    fun removeLayer(layer: PAGLayer): PAGLayer? = unpackLayerInfo(nativeRemoveLayer(nativeHandle, layer.nativeHandle, layer.internalLayerType))

    fun removeLayerAt(index: Int): PAGLayer? = unpackLayerInfo(nativeRemoveLayerAt(nativeHandle, index))

    fun removeAllLayers() = nativeRemoveAllLayers(nativeHandle)

    fun swapLayer(layer1: PAGLayer, layer2: PAGLayer) = nativeSwapLayer(nativeHandle, layer1.nativeHandle, layer1.internalLayerType, layer2.nativeHandle, layer2.internalLayerType)

    fun swapLayerAt(index1: Int, index2: Int) = nativeSwapLayerAt(nativeHandle, index1, index2)

    val audioBytes: ByteArray? get() = nativeAudioBytes(nativeHandle)

    val audioStartTime: Long get() = nativeAudioStartTime(nativeHandle)
}