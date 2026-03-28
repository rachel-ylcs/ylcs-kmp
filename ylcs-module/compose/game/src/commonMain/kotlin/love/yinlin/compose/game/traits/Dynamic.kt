package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable

@Stable
interface Dynamic {
    fun onUpdate(tick: Long)
}