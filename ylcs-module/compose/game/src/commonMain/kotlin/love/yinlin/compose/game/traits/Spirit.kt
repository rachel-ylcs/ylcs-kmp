package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.Manager
import love.yinlin.compose.game.Pointer

@Stable
abstract class Spirit(val manager: Manager): Soul {
    open fun update(tick: Long) {
        (this as? Dynamic)?.internalUpdate(tick) { onUpdate(it) }
    }

    open fun handlePointer(pointer: Pointer): Boolean {
        return (this as? PointerTrigger)?.internalHandlePointer(pointer) { onPointerEvent(it) } ?: false
    }

    open fun draw(drawer: Drawer) {
        (this as? Visible)?.internalDraw(drawer) { it.onDraw() }
    }
}