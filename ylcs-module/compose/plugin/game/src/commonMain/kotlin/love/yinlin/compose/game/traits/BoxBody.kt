package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
interface BoxBody : AABB {
    override operator fun contains(point: Offset): Boolean = (point.x >= 0f) && (point.x < size.width) && (point.y >= 0f) && (point.y < size.height)
}