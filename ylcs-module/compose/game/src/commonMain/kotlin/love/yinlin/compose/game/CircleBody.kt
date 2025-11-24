package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

@Stable
open class CircleBody : BoxBody {
    constructor(left: Float, top: Float, right: Float, bottom: Float) : super(left, top, right, bottom)
    constructor(rect: Rect) : super(rect.left, rect.top, rect.right, rect.bottom)
    constructor(topLeft: Offset, size: Size) : super(Rect(topLeft, size))

    override operator fun contains(point: Offset): Boolean {
        val a = (right - left) / 2
        val b = (bottom - top) / 2
        val dx = point.x - left - a
        val dy = point.y - top - b
        return (dx * dx) / (a * a) + (dy * dy) / (b * b) <= 1f
    }
}