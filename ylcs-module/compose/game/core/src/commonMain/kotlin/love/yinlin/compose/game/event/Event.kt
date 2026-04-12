package love.yinlin.compose.game.event

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import love.yinlin.compose.game.traits.Visible

@Stable
sealed interface Event {
    @Stable
    sealed interface Pointer : Event {
        val id: Long
        val position: Offset
        val source: Visible

        @Stable
        data class Down(
            override val id: Long,
            override val position: Offset,
            override val source: Visible
        ) : Pointer

        @Stable
        data class Up(
            override val id: Long,
            override val position: Offset,
            override val source: Visible,
            val originPosition: Offset
        ) : Pointer

        @Stable
        data class Move(
            override val id: Long,
            override val position: Offset,
            override val source: Visible,
            val originPosition: Offset
        ) : Pointer
    }
}