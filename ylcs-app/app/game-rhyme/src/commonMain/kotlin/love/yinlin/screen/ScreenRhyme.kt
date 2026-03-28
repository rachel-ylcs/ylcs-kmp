package love.yinlin.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.Viewport
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.traits.Visible
import love.yinlin.compose.screen.BasicScreen

@Stable
class ScreenRhyme : BasicScreen() {
    private val engine = Engine(Viewport.MatchHeight(3000))

    override suspend fun initialize() {
        if (engine.initialize()) {
            val scene = engine.plugin<ScenePlugin>()
            scene += object : Visible() {
                override fun Drawer.onDraw() {
                    scale(0.5f) {
                        rect(Colors.Green4, position = Offset.Zero, size = viewportSize)
                    }
                }
            }
        }
    }

    override fun finalize() {
        engine.release()
    }

    @Composable
    override fun BasicContent() {
        engine.ViewportContent(modifier = Modifier.fillMaxSize())
    }
}