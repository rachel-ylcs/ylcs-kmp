package love.yinlin.compose.ui.image

import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import love.yinlin.compose.image.input.ImageSource

@Stable
internal data class ImageGeometry(
    val source: ImageSource?,
    val version: String?,
    val ready: Boolean,
    val contentScale: ContentScale,
    val alignment: Alignment,
    val direction: LayoutDirection,
    val zoom: Boolean,
) {
    fun bounds(source: IntSize, viewport: IntSize): Rect {
        if (source.width <= 0 || source.height <= 0 || viewport.width <= 0 || viewport.height <= 0) return Rect.Zero
        val factor = contentScale.computeScaleFactor(source.toSize(), viewport.toSize())
        val size = Size(source.width * factor.scaleX, source.height * factor.scaleY)
        val offset = alignment.align(size.roundToIntSize(), viewport, direction)
        return Rect(offset.toOffset(), size)
    }
}
