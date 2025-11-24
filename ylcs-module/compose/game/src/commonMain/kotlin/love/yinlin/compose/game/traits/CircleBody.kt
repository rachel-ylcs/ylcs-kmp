package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
interface CircleBody : AABB {
    override operator fun contains(point: Offset): Boolean {
        val a = size.width / 2
        val b = size.height / 2
        val dx = point.x - a
        val dy = point.y - b
        return (dx * dx) / (a * a) + (dy * dy) / (b * b) <= 1f
    }
}