package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.common.Event

@Stable
interface Trigger {
    /**
     * 可交互
     */
    val interactive: Boolean get() = true

    /**
     * 事件处理
     */
    fun onEvent(tick: Long, event: Event): Boolean
}