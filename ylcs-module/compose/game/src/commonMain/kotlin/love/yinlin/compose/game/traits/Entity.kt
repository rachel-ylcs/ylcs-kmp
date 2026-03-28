package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Engine

@Stable
open class Entity {
    /**
     * 唯一标识
     */
    open val id: String? = null

    /**
     * 附加
     */
    open fun onAttached(engine: Engine) { }

    /**
     * 离开
     */
    open fun onDetached(engine: Engine) { }
}