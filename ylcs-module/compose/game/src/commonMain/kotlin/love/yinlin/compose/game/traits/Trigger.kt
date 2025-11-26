package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable

@Stable
interface Trigger {
    fun onEvent(event: Event): Boolean
}