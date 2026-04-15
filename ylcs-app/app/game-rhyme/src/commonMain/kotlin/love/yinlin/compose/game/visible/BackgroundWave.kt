package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Visible
import kotlin.math.sin

class BackgroundWave(
    val waveColor: Color,
    val phaseRatio: Float,
    val phi: Float,
    val frequency: Float,
    val amplitudeRatio: Float,
    override val layerOrder: Int
) : Visible(), Dynamic {
    var backgroundSize = Size.Zero
    val step = 20f
    var wavePhase = 0f
    var waveAmplitudeTime = 0f
    val wavePath = Path()

    override fun onUpdate(tick: Int) {
        val (w, h) = backgroundSize
        if (w == 0f || h == 0f) return

        val hy = h / 2
        val dynamicAmplitude = hy * (0.85f + 0.15f * sin(waveAmplitudeTime)) / 4
        var x = 0f
        wavePhase -= tick * phaseRatio
        waveAmplitudeTime += tick * 0.001f
        wavePath.reset()
        wavePath.moveTo(0f, hy + sin(wavePhase + phi) * dynamicAmplitude)
        while (x <= w) {
            wavePath.lineTo(x, hy + sin(x * frequency + wavePhase) * dynamicAmplitude * amplitudeRatio)
            x += step
        }
        wavePath.lineTo(w, hy + sin(w * frequency + wavePhase) * dynamicAmplitude * amplitudeRatio)

        updateDirty()
    }

    override fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) {
        backgroundSize = viewportSize
    }

    override fun Drawer.onDraw() {
        path(path = wavePath, color = waveColor, style = Stroke(width = 16f))
    }
}