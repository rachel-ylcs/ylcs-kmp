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

    open fun onPointerDown(event: Event.Pointer.Down) { }
    open fun onPointerUp(event: Event.Pointer.Up) { }
    open fun onPointerMove(event: Event.Pointer.Move) { }

    final override fun onEvent(event: Event, source: Visible?): Boolean {
        when (event) {
            is Event.Pointer.Down -> onPointerDown(event)
            is Event.Pointer.Up -> onPointerUp(event)
            is Event.Pointer.Move -> onPointerMove(event)
        }
        // 受击检测通过就必须消费完成
        return true
    }
}