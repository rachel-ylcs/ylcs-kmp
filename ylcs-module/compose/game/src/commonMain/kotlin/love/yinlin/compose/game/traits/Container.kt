package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMap
import love.yinlin.collection.PriorityQueue
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.Manager

@Stable
abstract class Container(manager: Manager) : Spirit(manager) {
    @Stable
    private data class SoulEntry(val soul: Soul, val addIndex: Long) {
        companion object {
            val comparator = Comparator<SoulEntry> { entry1, entry2 ->
                if (entry1.soul is Visible) {
                    if (entry2.soul is Visible) {
                        val result = entry1.soul.zIndex.compareTo(entry2.soul.zIndex)
                        if (result == 0) entry1.addIndex.compareTo(entry2.addIndex) else result
                    }
                    else -1
                }
                else {
                    if (entry2.soul is Visible) 1
                    else entry1.addIndex.compareTo(entry2.addIndex)
                }
            }
        }
    }

    protected abstract val souls: List<Soul>

    private var soulAddIndex: Long = 0L
    private val queue by lazy {
        PriorityQueue(SoulEntry.comparator).apply {
            push(souls.fastMap { soul -> SoulEntry(soul, soulAddIndex++) })
        }
    }

    // Dynamic
    protected open fun onClientPreUpdate(tick: Long) { }
    protected open fun onClientPostUpdate(tick: Long) { }
    final override fun onClientUpdate(tick: Long) {
        onClientPreUpdate(tick)
        if (queue.isNotEmpty) {
            for ((soul, _) in queue) (soul as? Dynamic)?.onUpdate(tick)
        }
        onClientPostUpdate(tick)
    }

    // Trigger
    protected open fun onClientPreEvent(): Boolean = false
    protected open fun onClientPostEvent(): Boolean = false

    final override fun onClientEvent(event: Event): Boolean {
        // 可以拦截子元素的事件
        if (onClientPreEvent()) return true
        if (queue.isNotEmpty) {
            // 点击事件与绘制流程是反的, zIndex越大的先响应
            for ((soul, _) in queue.reverse()) {
                if ((soul as? Trigger)?.onEvent(event) == true) return true
            }
        }
        return onClientPostEvent()
    }

    // Visible
    protected open fun Drawer.onClientPreDraw() { }
    protected open fun Drawer.onClientPostDraw() { }

    final override fun Drawer.onClientDraw() {
        onClientPreDraw()
        if (queue.isNotEmpty) {
            for ((soul, _) in queue) (soul as? Visible)?.apply { onDraw() }
        }
        onClientPostDraw()
    }
}