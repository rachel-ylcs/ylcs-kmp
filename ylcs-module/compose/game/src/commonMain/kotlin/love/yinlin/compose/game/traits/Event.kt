package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
sealed interface Event

@Stable
interface PointerEvent : Event {
    val id: Long
    val position: Offset

    fun reset(p: Offset): PointerEvent
}

@Stable
data class PointerDownEvent(override val id: Long, override val position: Offset) : PointerEvent {
    override fun reset(p: Offset): PointerEvent = PointerDownEvent(id, p)
}

@Stable
data class PointerUpEvent(override val id: Long, override val position: Offset) : PointerEvent {
    companion object {
        const val LONG_PRESS_TIMEOUT = 500L
    }

    override fun reset(p: Offset): PointerEvent = PointerUpEvent(id, p)
}