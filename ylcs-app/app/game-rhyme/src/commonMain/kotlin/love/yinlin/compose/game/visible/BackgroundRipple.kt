package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.Colors
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Visible
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class BackgroundRipple(override val layerOrder: Int) : Visible(), Dynamic {
    var backgroundSize = Size.Zero
    var resonanceTime = 0f
    var resonanceUpdateTimer = 0f
    val resonancePath = Path()
    val resonanceColor = Colors(0xFF7C4DFF).copy(alpha = 0.15f)
    var resonanceTargetScale = 1f
    var resonanceCurrentScale = 1f
    val edgeNoises = FloatArray(50)
    val noiseSize = edgeNoises.size

    override fun onUpdate(tick: Int) {
        val (centerX, centerY) = backgroundSize.center
        if (centerX == 0f || centerY == 0f) return

        resonanceTime += tick * 0.01f
        resonanceUpdateTimer += tick
        resonanceCurrentScale += (resonanceTargetScale - resonanceCurrentScale) * (tick * 0.01f).coerceIn(0f, 1f)

        if (resonanceUpdateTimer >= 96f) {
            resonanceUpdateTimer = 0f
            resonanceTargetScale = 0.9f + Random.nextFloat() * 0.3f
            repeat(noiseSize) { i ->
                edgeNoises[i] = sin(resonanceTime * 5 + i) * 10 + Random.nextFloat() * 8f
            }
        }

        val currentBaseRadius = min(centerX, centerY) * resonanceCurrentScale / 2
        resonancePath.reset()
        repeat(noiseSize) { i ->
            val angleRad = i * 6.283184f / noiseSize
            val r = currentBaseRadius + edgeNoises[i]
            val px = centerX + r * cos(angleRad)
            val py = centerY + r * sin(angleRad)
            if (i == 0) resonancePath.moveTo(px, py) else resonancePath.lineTo(px, py)
        }
        resonancePath.close()

        updateDirty()
    }

    override fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) {
        backgroundSize = viewportSize
    }

    override fun Drawer.onDraw() {
        path(path = resonancePath, color = resonanceColor, style = Stroke(width = 20f))
    }
}