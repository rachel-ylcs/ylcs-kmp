package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
interface AABB : Positionable {
    operator fun contains(point: Offset): Boolean
}