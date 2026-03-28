package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable

@Stable
interface Dynamic {
    val active: Boolean get() = true
    fun onUpdate(tick: Long)
}