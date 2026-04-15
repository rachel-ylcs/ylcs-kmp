package love.yinlin.compose.game.event

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.traits.Visible

@Stable
sealed interface Event {
    @Stable
    sealed interface Pointer : Event {
        val id: Long
        val position: Offset
        val layer: Layer
        val source: Visible?
        val arg: Any

        @Stable
        data class Down(
            override val id: Long,
            override val position: Offset,
            override val layer: Layer,
            override val source: Visible?,
            override val arg: Any = Unit,
        ) : Pointer

        @Stable
        data class Up(
            override val id: Long,
            override val position: Offset,
            override val layer: Layer,
            override val source: Visible?,
            val originPosition: Offset,
            override val arg: Any = Unit,
        ) : Pointer

        @Stable
        data class Move(
            override val id: Long,
            override val position: Offset,
            override val layer: Layer,
            override val source: Visible?,
            val originPosition: Offset,
            override val arg: Any = Unit,
        ) : Pointer
    }
}