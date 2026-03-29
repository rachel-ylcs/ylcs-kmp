package love.yinlin.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.app.global.resources.xwwk
import love.yinlin.app.game_rhyme.resources.Res as RhymeRes
import love.yinlin.app.global.resources.Res as GlobalRes
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.Viewport
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.plugin.FontPlugin
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.traits.Visible
import love.yinlin.compose.screen.BasicScreen

@Stable
class ScreenRhyme : BasicScreen() {
    private val engine = Engine(
        Viewport.MatchHeight(1000),
        FontPlugin.resources(
            GlobalRes.font.xwwk,
            RhymeRes.font.rhyme,
        ),
        ::ScenePlugin
    )

    override suspend fun initialize() {
        if (engine.initialize()) {
            val scene = engine.plugin<ScenePlugin>()
            val rect = object : Visible(Offset.Zero, Size(300f, 300f)) {
                override fun Drawer.onDraw(viewportSize: Size) {
                    rect(Colors.Green4, position = Offset.Zero, size = size)
                }
            }
            val layer = Layer(rect)
            scene += layer
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