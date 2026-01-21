@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import platform.CoreGraphics.CGSizeMake

actual open class PAGComposition(override val delegate: PlatformPAGComposition) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(width: Int, height: Int): PAGComposition =
            PAGComposition(PlatformPAGComposition.Make(CGSizeMake(width.toDouble(), height.toDouble()))!!)
    }

    actual val width: Int get() = delegate.width().toInt()
    actual val height: Int get() = delegate.height().toInt()
    actual fun setContentSize(width: Int, height: Int) { delegate.setContentSize(CGSizeMake(width.toDouble(), height.toDouble())) }
    actual val numChildren: Int get() = delegate.numChildren().toInt()
    actual fun getLayerAt(index: Int): PAGLayer? = delegate.getLayerAt(index)?.let(::PAGLayer)
    actual fun getLayerIndex(layer: PAGLayer): Int = delegate.getLayerIndex(layer.delegate).toInt()
    actual fun setLayerIndex(layer: PAGLayer, index: Int) { delegate.setLayerIndex(index.toLong(), layer.delegate) }
    actual fun addLayer(layer: PAGLayer) { delegate.addLayer(layer.delegate) }
    actual fun addLayerAt(layer: PAGLayer, index: Int) { delegate.addLayer(layer.delegate, index) }
    actual operator fun contains(layer: PAGLayer): Boolean = delegate.contains(layer.delegate)
    actual fun removeLayer(layer: PAGLayer): PAGLayer? = delegate.removeLayer(layer.delegate)?.let(::PAGLayer)
    actual fun removeLayerAt(index: Int): PAGLayer? = delegate.removeLayerAt(index)?.let(::PAGLayer)
    actual fun removeAllLayers() { delegate.removeAllLayers() }
    actual fun swapLayer(layer1: PAGLayer, layer2: PAGLayer) { delegate.swapLayer(layer1.delegate, layer2.delegate) }
    actual fun swapLayerAt(index1: Int, index2: Int) { delegate.swapLayerAt(index1, index2) }
    actual val audioStartTime: Long get() = delegate.audioStartTime()
}