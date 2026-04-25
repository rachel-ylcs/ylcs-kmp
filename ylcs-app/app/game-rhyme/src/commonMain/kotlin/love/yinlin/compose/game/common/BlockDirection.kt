package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
enum class BlockDirection {
    UP, DOWN, LEFT, RIGHT;

    companion object {
        fun calculate(p1: Offset, p2: Offset): BlockDirection = when {
            p1.x > p2.x -> RIGHT
            p1.x < p2.x -> LEFT
            p1.y > p2.y -> DOWN
            else -> UP
        }
    }
}