package love.yinlin.compose.game.event

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.traits.Visible
import kotlin.reflect.KClass

@Stable
open class PointerEventListener : EventListener {
    final override val target: Array<KClass<out Event>> = arrayOf(
        Event.Pointer::class,
        Event.Pointer.Down::class,
        Event.Pointer.Up::class,
        Event.Pointer.Move::class
    )

    open fun onPointerDown(tick: Long, event: Event.Pointer.Down): Boolean = false
    open fun onPointerUp(tick: Long, event: Event.Pointer.Up): Boolean = false
    open fun onPointerMove(tick: Long, event: Event.Pointer.Move): Boolean = false

    override fun onEvent(tick: Long, event: Event, source: Visible): Boolean = when (event) {
        is Event.Pointer.Down -> {
            val newPosition = event.position - source.position + source.center
            val newEvent = event.copy(position = newPosition)
            source.aabb.contains(source.size, newPosition) && onPointerDown(tick, newEvent)
        }
        is Event.Pointer.Up -> {
            val newPosition = event.origin - source.position + source.center
            val newEvent = event.copy(position = newPosition)
            source.aabb.contains(source.size, newPosition) && onPointerUp(tick, newEvent)
        }
        is Event.Pointer.Move -> {
            val newPosition = event.origin - source.position + source.center
            val newEvent = event.copy(position = newPosition)
            source.aabb.contains(source.size, newPosition) && onPointerMove(tick, newEvent)
        }
        else -> false
    }
}