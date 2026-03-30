package love.yinlin.compose.game.event

import androidx.compose.runtime.Stable
import kotlin.reflect.KClass

@Stable
open class CombinedPointerEventListener : EventListener {
    override val target: Array<KClass<out Event>> = arrayOf(
        Event.Pointer::class,
        Event.Pointer.Down::class,
        Event.Pointer.Up::class,
        Event.Pointer.Move::class
    )

    open fun onPointer(tick: Long, event: Event.Pointer): Boolean = false

    final override fun onEvent(tick: Long, event: Event): Boolean = if (event is Event.Pointer) onPointer(tick, event) else false
}