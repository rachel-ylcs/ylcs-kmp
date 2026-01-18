package org.libpag

internal fun unpackLayerInfo(layoutInfo: LongArray): PAGLayer? {
    val handle = layoutInfo[0]
    val type = layoutInfo[1]
    if (handle == 0L) return null
    return when (type) {
        PAGLayerType.File.value -> PAGFile { handle }
        PAGLayerType.Solid.value -> PAGSolidLayer { handle }
        PAGLayerType.Text.value -> PAGTextLayer { handle }
        PAGLayerType.Shape.value -> PAGShapeLayer { handle }
        PAGLayerType.Image.value -> PAGImageLayer { handle }
        PAGLayerType.PreCompose.value -> PAGComposition { handle }
        else -> PAGLayer { handle }
    }
}