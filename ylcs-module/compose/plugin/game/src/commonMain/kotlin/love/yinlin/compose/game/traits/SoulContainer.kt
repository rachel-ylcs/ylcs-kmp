package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.Manager
import love.yinlin.compose.game.SoulList

@Stable
abstract class SoulContainer(manager: Manager) : Spirit(manager) {
    protected abstract val souls: List<Soul>

    private val queue by lazy { SoulList(souls) }

    // Dynamic
    protected open fun onClientPreUpdate(tick: Long) { }
    protected open fun onClientPostUpdate(tick: Long) { }
    final override fun onClientUpdate(tick: Long) {
        onClientPreUpdate(tick)
        queue.forEachForward { (it as? Dynamic)?.onUpdate(tick) }
        onClientPostUpdate(tick)
    }

    // Trigger
    protected open fun onClientPreEvent(): Boolean = false
    protected open fun onClientPostEvent(): Boolean = false

    final override fun onClientEvent(tick: Long, event: Event): Boolean {
        // 可以拦截子元素的事件
        if (onClientPreEvent()) return true
        queue.forEachReverse {
            if ((it as? Trigger)?.onEvent(tick, event) == true) return true
        }
        return onClientPostEvent()
    }

    // Visible
    protected open fun Drawer.onClientPreDraw() { }
    protected open fun Drawer.onClientPostDraw() { }

    final override fun Drawer.onClientDraw() {
        onClientPreDraw()
        queue.forEachForward { (it as? Visible)?.apply { onDraw() } }
        onClientPostDraw()
    }
}