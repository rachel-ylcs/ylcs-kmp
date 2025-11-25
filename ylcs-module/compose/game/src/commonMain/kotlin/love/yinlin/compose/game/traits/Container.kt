package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMap
import love.yinlin.collection.PriorityQueue
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.Manager
import love.yinlin.compose.game.Pointer

@Stable
abstract class Container(manager: Manager) : Spirit(manager), Dynamic, PointerTrigger, Visible {
    protected abstract val spirits: List<Spirit>

    protected open fun preUpdate(tick: Long) { }
    protected open fun Drawer.preDraw() { }

    override fun onUpdate(tick: Long) { }
    override fun onPointerEvent(pointer: Pointer): Boolean = false
    override fun Drawer.onDraw() { }

    @Stable
    private data class SpiritEntry(val spirit: Spirit, val addIndex: Long) {
        companion object {
            val comparator = Comparator<SpiritEntry> { entry1, entry2 ->
                if (entry1.spirit is Visible) {
                    if (entry2.spirit is Visible) {
                        val result = entry1.spirit.zIndex.compareTo(entry2.spirit.zIndex)
                        if (result == 0) entry1.addIndex.compareTo(entry2.addIndex) else result
                    }
                    else -1
                }
                else {
                    if (entry2.spirit is Visible) 1
                    else entry1.addIndex.compareTo(entry2.addIndex)
                }
            }
        }
    }

    private var spiritAddIndex: Long = 0L
    private val queue by lazy {
        PriorityQueue(SpiritEntry.comparator).apply {
            push(spirits.fastMap { spirit -> SpiritEntry(spirit, spiritAddIndex++) })
        }
    }

    override fun update(tick: Long) {
        internalUpdate(tick) {
            preUpdate(it)
            if (queue.isNotEmpty) {
                for ((spirit, _) in queue) spirit.update(it)
            }
            onUpdate(it)
        }
    }

    override fun handlePointer(pointer: Pointer): Boolean {
        return internalHandlePointer(pointer) {
            if (onPointerEvent(it)) true
            else {
                if (queue.isNotEmpty) {
                    for ((spirit, _) in queue.reverse()) {
                        if (spirit.handlePointer(it)) return@internalHandlePointer true
                    }
                }
                false
            }
        }
    }

    override fun draw(drawer: Drawer) {
        internalDraw(drawer) {
            it.preDraw()
            if (queue.isNotEmpty) {
                for ((spirit, _) in queue) spirit.draw(it)
            }
            it.onDraw()
        }
    }
}