package love.yinlin.compose.game.common

import love.yinlin.compose.game.traits.Visible

class Moments(vararg moments: Moment) {
    @PublishedApi internal val items = moments.toMutableList()

    inline fun check(tick: Long, crossinline block: (Visible) -> Unit) {
        items.removeAll { moment ->
            if (tick >= moment.start) {
                block(moment.builder())
                true
            }
            else false
        }
    }
}