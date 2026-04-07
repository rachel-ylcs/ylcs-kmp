package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable

@Stable
interface Dynamic {
    /**
     * 激活更新
     */
    val active: Boolean get() = true

    /**
     * 更新
     */
    fun onUpdate(tick: Int)
}