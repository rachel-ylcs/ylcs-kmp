package io.github.alexzhirkevich.qrose.options

import androidx.compose.ui.graphics.Path

fun interface QrShapeModifier {

    /**
     * Modify current path or create new one.
     *
     * Receiver path is empty and reused for optimization.
     * Most benefit this optimization gives when the shape is used for pixels with [QrBrushMode.Separate].
     *
     * */
    fun Path.path(size : Float, neighbors: Neighbors) : Path
}

internal fun QrShapeModifier.newPath(size: Float, neighbors: Neighbors) : Path = Path().apply {
    path(size, neighbors)
}