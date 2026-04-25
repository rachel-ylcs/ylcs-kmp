package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastCoerceAtLeast
import love.yinlin.app.game_rhyme.resources.Res
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.compose.Colors
import love.yinlin.compose.animation.Interpolator
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.drawer.TextGraph
import kotlin.math.abs
import kotlin.math.sin

class InteractTipArea : MomentVisible(12000) {
    private var area: Array<Offset> = Array(7) { Offset.Zero }
    private var areaSize: Size = Size.Zero
    private var noteFontMap: List<TextGraph>? = null

    private var alpha: Float = 0f

    override fun onUpdateMoment(progress: Float) {
        alpha = (0.1f * abs(sin(progress * 3 * Interpolator.PI))).fastCoerceAtLeast(0f)
    }

    override fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) {
        val (w, h) = viewportSize
        val w0 = 0f
        val w1 = w / 3
        val w2 = w * 2 / 3
        val h0 = if (w >= h) 0f else h / 2
        val h1 = if (w >= h) h / 3 else h * 2 / 3
        val h2 = if (w >= h) h * 2 / 3 else h * 5 / 6
        areaSize = Size(w1 - w0, h1 - h0)
        area[0] = Offset(w0, h0)
        area[1] = Offset(w0, h1)
        area[2] = Offset(w0, h2)
        area[3] = Offset(w1, h2)
        area[4] = Offset(w2, h2)
        area[5] = Offset(w2, h1)
        area[6] = Offset(w2, h0)

        if (noteFontMap == null) {
            noteFontMap = List(7) {
                measureText((it + 1).toString(), font = Res.font.rhyme, fontWeight = FontWeight.Bold)
            }
        }
    }

    override fun Drawer.onDraw() {
        noteFontMap?.let { graphs ->
            val centerOffset = areaSize.center / 2f
            val textSize = areaSize / 2f
            val textColor = Colors.White.copy(alpha = alpha)

            repeat(7) { index ->
                val color = Block.ScaleColorList[index + 1]
                val topLeft = area[index]

                rect(color, topLeft, areaSize, alpha = alpha)
                text(graphs[index], topLeft + centerOffset, textSize, textColor, textAlign = TextAlign.Center)
            }
        }
    }
}