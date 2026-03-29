package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawTransform

@Stable
interface Shape {
    fun contains(size: Size, point: Offset): Boolean
    fun onClip(transform: DrawTransform, size: Size)

    @Stable
    object Box : Shape {
        override fun contains(size: Size, point: Offset): Boolean {
            return (point.x >= 0f) && (point.x < size.width) && (point.y >= 0f) && (point.y < size.height)
        }

        override fun onClip(transform: DrawTransform, size: Size) {
            transform.clipRect(0f, 0f, size.width, size.height)
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

        override fun onClip(transform: DrawTransform, size: Size) {
            transform.clipPath(Path().apply { addOval(Rect(Offset.Zero, size)) })
        }
    }
}