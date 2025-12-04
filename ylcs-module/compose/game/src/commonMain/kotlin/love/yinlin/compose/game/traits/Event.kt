package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
sealed interface Event

@Stable
sealed interface PointerEvent : Event {
    val id: Long
    val position: Offset
}

// 指针按下
@Stable
data class PointerDownEvent(override val id: Long, override val position: Offset) : PointerEvent {
    fun reset(p: Offset) = PointerDownEvent(id, p)
}

// 指针抬起
@Stable
data class PointerUpEvent(override val id: Long, override val position: Offset, val rawPosition: Offset) : PointerEvent {
    companion object {
        const val LONG_PRESS_TIMEOUT = 500L
    }

    fun reset(p: Offset, raw: Offset) = PointerUpEvent(id, p, raw)
}

// 指针偏移
@Stable
data class PointerMoveEvent(override val id: Long, override val position: Offset, val rawPosition: Offset): PointerEvent {
    fun reset(p: Offset, raw: Offset) = PointerMoveEvent(id, p, raw)
}