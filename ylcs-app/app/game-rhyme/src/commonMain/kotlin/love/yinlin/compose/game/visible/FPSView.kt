package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import love.yinlin.app.game_rhyme.resources.Res
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.compose.Colors
import love.yinlin.compose.game.common.FPSCounter
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.drawer.TextGraph
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Visible

class FPSView(
    override val layerOrder: Int,
    duration: Int
) : Visible(), Dynamic {
    companion object {
        private const val DEFAULT_HEIGHT = 40f
        private val DefaultColor = Colors.White.copy(alpha = 0.3f)
    }

    private val fpsCounter = FPSCounter(duration)
    private val fpsCache = mutableMapOf<Int, TextGraph>()
    private var currentFpsGraph: TextGraph? = null
    private var fpsTextWidth: Float = 0f
    private var fpsTextBuilder: ((Int) -> TextGraph)? = null

    override fun onUpdate(tick: Int) {
        fpsCounter.update(tick) { fps ->
            fpsTextBuilder?.let { builder ->
                if (fpsCache.size >= 16) fpsCache.clear()
                currentFpsGraph = fpsCache.getOrPut(fps) { builder(fps) }
                updateDirty()
            }
        }
    }

    override fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) {
        fpsTextWidth = viewportSize.width
        if (fpsTextBuilder == null) fpsTextBuilder = { fps -> measureText("FPS: $fps", Res.font.rhyme, FontWeight.Bold) }
    }

    override fun Drawer.onDraw() {
        currentFpsGraph?.let { graph ->
            text(graph, Offset.Zero, Size(fpsTextWidth, DEFAULT_HEIGHT), DefaultColor, TextAlign.Center)
        }
    }
}