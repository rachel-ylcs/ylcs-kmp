package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
expect open class PAGComposition : PAGLayer {
    companion object {
        fun make(width: Int, height: Int): PAGComposition
    }

    val width: Int
    val height: Int
    fun setContentSize(width: Int, height: Int)
    val numChildren: Int
    fun getLayerAt(index: Int): PAGLayer?
    fun getLayerIndex(layer: PAGLayer): Int
    fun setLayerIndex(layer: PAGLayer, index: Int)
    fun addLayer(layer: PAGLayer)
    fun addLayerAt(layer: PAGLayer, index: Int)
    operator fun contains(layer: PAGLayer): Boolean
    fun removeLayer(layer: PAGLayer): PAGLayer?
    fun removeLayerAt(index: Int): PAGLayer?
    fun removeAllLayers()
    fun swapLayer(layer1: PAGLayer, layer2: PAGLayer)
    fun swapLayerAt(index1: Int, index2: Int)
    val audioStartTime: Long
}