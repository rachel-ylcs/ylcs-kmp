package love.yinlin.compose.game.event

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
sealed interface Event {
    @Stable
    sealed interface Pointer : Event {
        val position: Offset

        @Stable
        data class Down(override val position: Offset) : Pointer

        @Stable
        data class Up(override val position: Offset, val origin: Offset) : Pointer

        @Stable
        data class Move(override val position: Offset, val origin: Offset) : Pointer
    }
}