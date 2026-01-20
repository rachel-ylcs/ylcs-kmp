package love.yinlin.compose.ui

actual open class PAGComposition(override val delegate: PlatformPAGComposition) : PAGLayer(delegate) {
    actual companion object {
        actual fun make(width: Int, height: Int): PAGComposition =
            PAGComposition(PlatformPAGComposition.make(width, height))
    }

    actual val width: Int get() = delegate.width()
    actual val height: Int get() = delegate.height()
    actual fun setContentSize(width: Int, height: Int) = delegate.setContentSize(width, height)
    actual val numChildren: Int get() = delegate.numChildren()
    actual fun getLayerAt(index: Int): PAGLayer? = delegate.getLayerAt(index)?.let(::PAGLayer)
    actual fun getLayerIndex(layer: PAGLayer): Int = delegate.getLayerIndex(layer.delegate)
    actual fun setLayerIndex(layer: PAGLayer, index: Int) { delegate.setLayerIndex(layer.delegate, index) }
    actual fun addLayer(layer: PAGLayer) { delegate.addLayer(layer.delegate) }
    actual fun addLayerAt(layer: PAGLayer, index: Int) { delegate.addLayerAt(layer.delegate, index) }
    actual operator fun contains(layer: PAGLayer): Boolean = delegate.contains(layer.delegate)
    actual fun removeLayer(layer: PAGLayer): PAGLayer? = delegate.removeLayer(layer.delegate)?.let(::PAGLayer)
    actual fun removeLayerAt(index: Int): PAGLayer? = delegate.removeLayerAt(index)?.let(::PAGLayer)
    actual fun removeAllLayers() = delegate.removeAllLayers()
    actual fun swapLayer(layer1: PAGLayer, layer2: PAGLayer) = delegate.swapLayer(layer1.delegate, layer2.delegate)
    actual fun swapLayerAt(index1: Int, index2: Int) = delegate.swapLayerAt(index1, index2)
    actual val audioStartTime: Long get() = delegate.audioStartTime().toLong()
}