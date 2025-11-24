package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

@Stable
interface AABB {
    val left: Float
    val top: Float
    val right: Float
    val bottom: Float
    val topLeft: Offset
    val size: Size
    val rect: Rect
    val center: Offset
    val radius: Float

    operator fun contains(point: Offset): Boolean
}