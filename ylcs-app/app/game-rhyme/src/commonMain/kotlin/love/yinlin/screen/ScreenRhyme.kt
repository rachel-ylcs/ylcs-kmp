package love.yinlin.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.app.global.resources.xwwk
import love.yinlin.app.game_rhyme.resources.Res as RhymeRes
import love.yinlin.app.global.resources.Res as GlobalRes
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Viewport
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.common.PrepareDrawer
import love.yinlin.compose.game.common.TextGraph
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.event.PointerEventListener
import love.yinlin.compose.game.plugin.FontPlugin
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.traits.Trigger
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
        ScenePlugin.build()
    )

    override suspend fun initialize() {
        if (engine.initialize()) {
            val scene = engine.plugin<ScenePlugin>()
            val rect = object : Visible(Offset.Zero, Size(800f, 100f)) {
                override val trigger: Trigger = Trigger(
                    object : PointerEventListener() {
                        override fun onPointerDown(tick: Long, event: Event.Pointer.Down): Boolean {
                            println("${event}")
                            return true
                        }
                    }
                )

                override fun Drawer.onDraw() {
                    rect(Colors.Green4, position = Offset.Zero, size = size)
                }
            }
            val text = object : Visible(Offset.Zero, Size(800f, 100f)) {
                var textGraph: TextGraph? = null

                override fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) {
                    textGraph = measureText("Hello 你好", GlobalRes.font.xwwk)
                }

                override fun Drawer.onDraw() {
                    text(textGraph, Offset.Zero, size, Colors.White)
                }
            }
            scene += Layer(rect, text)
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