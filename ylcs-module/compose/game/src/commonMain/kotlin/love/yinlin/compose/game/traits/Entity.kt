package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Engine

@Stable
open class Entity {
    open val id: String? = null
    open fun onAttached(engine: Engine) { }
    open fun onDetached(engine: Engine) { }
}