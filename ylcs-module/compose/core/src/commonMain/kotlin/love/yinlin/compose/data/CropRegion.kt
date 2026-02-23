package love.yinlin.compose.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

@Stable
data class CropRegion(
    val xPercent: Float,
    val yPercent: Float,
    val widthPercent: Float,
    val heightPercent: Float
) {
    constructor(rect: Rect, container: Size) : this(
        xPercent = (rect.left / container.width).coerceIn(0f, 1f),
        yPercent = (rect.top / container.height).coerceIn(0f, 1f),
        widthPercent = (rect.width / container.width).coerceIn(0f, 1f),
        heightPercent = (rect.height / container.height).coerceIn(0f, 1f),
    )

    override fun toString(): String = "CropRegion($xPercent, $yPercent, $widthPercent, $heightPercent)"

    operator fun times(container: Size) = Rect(
        offset = Offset(container.width * xPercent,  container.height * yPercent),
        size = Size(container.width * widthPercent, container.height * heightPercent)
    )

    companion object {
        val Center = CropRegion(0.25f, 0.25f, 0.5f, 0.5f)
    }
}