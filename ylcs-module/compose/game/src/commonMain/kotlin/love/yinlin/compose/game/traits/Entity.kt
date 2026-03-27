package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Engine

@Stable
interface Entity {
    val id: String?
    fun onAttached(engine: Engine)
    fun onDetached(engine: Engine)
}