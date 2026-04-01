package love.yinlin.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.app.global.resources.img_logo
import love.yinlin.app.global.resources.xwwk
import love.yinlin.app.game_rhyme.resources.Res as RhymeRes
import love.yinlin.app.global.resources.Res as GlobalRes
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.asset.ResourceImageLoader
import love.yinlin.compose.game.viewport.Viewport
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.InitialDrawer
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.drawer.TextGraph
import love.yinlin.compose.game.event.Event
import love.yinlin.compose.game.event.PointerEventListener
import love.yinlin.compose.game.plugin.AssetPlugin
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
        FontPlugin.ResourceFactory(
            GlobalRes.font.xwwk,
            RhymeRes.font.rhyme,
        ),
        AssetPlugin.DefaultFactory(
            ResourceImageLoader(
                GlobalRes.drawable.img_logo
            )
        ),
        ScenePlugin.DefaultFactory()
    )

    override suspend fun initialize() {
        if (engine.initialize()) {
            val scene = engine.plugin<ScenePlugin>()

            val bound = object : Visible(Offset.Zero, Size.Zero, useCulling = false) {
                var dynamicBound: Rect = Rect.Zero

                override val clip: Boolean = false

                override fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) {
                    dynamicBound = Rect(-viewportSize.center, viewportSize)
                }

                override fun Drawer.onDraw() {
                    line(Colors.Red4, dynamicBound.topCenter, dynamicBound.bottomCenter, Stroke(3f))
                    line(Colors.Red4, dynamicBound.centerLeft, dynamicBound.centerRight, Stroke(3f))
                }
            }

            val rect = object : Visible(Offset.Zero, Size(800f, 100f)) {
                override val trigger: Trigger = Trigger(
                    object : PointerEventListener() {
                        override fun onPointerDown(tick: Long, event: Event.Pointer.Down): Boolean {
                            size = size.copy(width = size.width - 100f)
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

                override fun InitialDrawer.initializeDraw(viewportSize: Size, viewportBounds: Rect) {
                    textGraph = measureText("Hello 你好", GlobalRes.font.xwwk)
                }

                override fun Drawer.onDraw() {
                    textGraph?.let { text(it, Offset.Zero, size, Colors.White, textAlign = TextAlign.Center) }
                }
            }

            val image = object : Visible(Offset(200f, 200f), Size(400f, 400f)) {
                var bitmap: ImageBitmap? = null

                override fun InitialDrawer.initializeDraw(viewportSize: Size, viewportBounds: Rect) {
                    bitmap = assetProvider[GlobalRes.drawable.img_logo]
                }

                override fun Drawer.onDraw() {
                    bitmap?.let { image(it, Offset.Zero, size) }
                }
            }

            scene += Layer(bound, rect, text, image)
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