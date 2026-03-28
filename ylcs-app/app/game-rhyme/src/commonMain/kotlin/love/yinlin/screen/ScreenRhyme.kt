package love.yinlin.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.delay
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.Viewport
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.traits.Visible
import love.yinlin.compose.screen.BasicScreen

@Stable
class ScreenRhyme : BasicScreen() {
    private val engine = Engine(Viewport.MatchHeight(1000))

    override suspend fun initialize() {
        if (engine.initialize()) {
            val scene = engine.plugin<ScenePlugin>()
            scene += object : Visible(Offset.Zero, Size(300f, 300f)) {
                override fun Drawer.onDraw() {
                    rect(Colors.Green4, position = Offset.Zero, size = size)
                }
            }
            while (true) {
                scene.camera.updatePosition { it.copy(y = it.y + 10) }
                delay(16L)
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