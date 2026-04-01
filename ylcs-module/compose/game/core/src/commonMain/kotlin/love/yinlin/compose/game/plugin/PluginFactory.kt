package love.yinlin.compose.game.plugin

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.Engine

@Stable
interface PluginFactory {
    fun build(engine: Engine): Plugin
}