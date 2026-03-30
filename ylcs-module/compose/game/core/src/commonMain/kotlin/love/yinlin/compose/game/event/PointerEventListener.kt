package love.yinlin.compose.game.event

import androidx.compose.runtime.Stable
import kotlin.reflect.KClass

@Stable
open class PointerEventListener : EventListener {
    override val target: Array<KClass<out Event>> = arrayOf(
        Event.Pointer::class,
        Event.Pointer.Down::class,
        Event.Pointer.Up::class,
        Event.Pointer.Move::class
    )

    open fun onPointerDown(tick: Long, event: Event.Pointer.Down): Boolean = false
    open fun onPointerUp(tick: Long, event: Event.Pointer.Up): Boolean = false
    open fun onPointerMove(tick: Long, event: Event.Pointer.Move): Boolean = false

    final override fun onEvent(tick: Long, event: Event): Boolean = when (event) {
        is Event.Pointer.Down -> onPointerDown(tick, event)
        is Event.Pointer.Up -> onPointerUp(tick, event)
        is Event.Pointer.Move -> onPointerMove(tick, event)
        else -> false
    }
}