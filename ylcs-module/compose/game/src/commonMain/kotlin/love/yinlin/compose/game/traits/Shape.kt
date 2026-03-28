package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.common.Drawer

@Stable
interface Shape {
    fun contains(size: Size, point: Offset): Boolean
    fun Drawer.onClip(size: Size)

    @Stable
    object Box : Shape {
        override fun contains(size: Size, point: Offset): Boolean {
            return (point.x >= 0f) && (point.x < size.width) && (point.y >= 0f) && (point.y < size.height)
        }

        override fun Drawer.onClip(size: Size) {

        }
    }

    @Stable
    object Circle : Shape {
        override fun contains(size: Size, point: Offset): Boolean {
            val a = size.width / 2
            val b = size.height / 2
            val dx = point.x - a
            val dy = point.y - b
            return (dx * dx) / (a * a) + (dy * dy) / (b * b) <= 1f
        }

        override fun Drawer.onClip(size: Size) {

        }
    }
}