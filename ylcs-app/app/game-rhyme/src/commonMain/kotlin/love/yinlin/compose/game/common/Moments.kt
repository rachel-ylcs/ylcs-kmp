package love.yinlin.compose.game.common

import love.yinlin.compose.game.traits.Visible

class Moments(momentBuilder: Scope.() -> Unit) {
    class Scope internal constructor() {
        internal val moments: MutableList<Pair<Long, () -> Visible>> = []

        fun moment(start: Long, builder: () -> Visible) { moments += start to builder }
    }

    @PublishedApi internal val items = Scope().also(momentBuilder).moments

    inline fun check(tick: Long, crossinline block: (Visible) -> Unit) {
        if (items.isEmpty()) return
        items.removeAll { [momentTick, visible] ->
            if (tick >= momentTick) {
                block(visible())
                true
            }
            else false
        }
    }
}