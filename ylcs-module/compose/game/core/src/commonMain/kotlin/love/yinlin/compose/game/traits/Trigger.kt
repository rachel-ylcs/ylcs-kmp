package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.event.EventListener
import kotlin.reflect.KClass

@Stable
class Trigger(vararg userListeners: EventListener) {
    @PublishedApi internal val listeners = mutableMapOf<KClass<out Event>, EventListener>()

    init {
        for (listener in userListeners) listener.target.forEach { listeners[it] = listener }
    }

    inline operator fun <reified T : EventListener> plusAssign(listener: T) {
        listener.target.forEach { listeners[it] = listener }
    }

    inline operator fun <reified T : EventListener> minusAssign(listener: T) {
        listener.target.forEach { listeners.remove(it) }
    }

    internal fun onEvent(tick: Long, event: Event, source: Visible): Boolean = listeners[event::class]?.onEvent(tick, event, source) ?: false
}