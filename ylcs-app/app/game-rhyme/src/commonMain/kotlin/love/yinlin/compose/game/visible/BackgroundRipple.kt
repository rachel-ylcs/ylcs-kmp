package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.fastCoerceIn
import love.yinlin.compose.Colors
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Visible
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class BackgroundRipple(
    private val character: Character,
    private val characterImage: ImageBitmap?,
    override val layerOrder: Int,
) : Visible(), Dynamic {
    var backgroundSize = Size.Zero
    var resonanceTime = 0f
    var resonanceUpdateTimer = 0f
    val resonancePath = Path()
    val resonanceColor = Color(0xFF7C4DFF)
    var resonanceTargetScale = 1f
    var resonanceCurrentScale = 1f
    val edgeNoises = FloatArray(50)
    val noiseSize = edgeNoises.size
    var characterBounds = Rect.Zero

    var skillTick: Int = 0
    var skillAlpha: Float = 0.2f
    var skillOpened: Boolean = false
    val skillDuration: Int = 300

    override fun onUpdate(tick: Int) {
        val (centerX, centerY) = backgroundSize.center
        if (centerX == 0f || centerY == 0f) return

        resonanceTime += tick * 0.01f
        resonanceUpdateTimer += tick
        resonanceCurrentScale += (resonanceTargetScale - resonanceCurrentScale) * (tick * 0.01f).fastCoerceIn(0f, 1f)

        if (resonanceUpdateTimer >= 96f) {
            resonanceUpdateTimer = 0f
            resonanceTargetScale = 0.9f + Random.nextFloat() * 0.3f
            repeat(noiseSize) { i ->
                edgeNoises[i] = sin(resonanceTime * 5 + i) * 10 + Random.nextFloat() * 8f
            }
        }

        val currentBaseRadius = minOf(centerX, centerY) * resonanceCurrentScale / 2
        resonancePath.reset()
        repeat(noiseSize) { i ->
            val angleRad = i * 6.283184f / noiseSize
            val r = currentBaseRadius + edgeNoises[i]
            val px = centerX + r * cos(angleRad)
            val py = centerY + r * sin(angleRad)
            if (i == 0) resonancePath.moveTo(px, py) else resonancePath.lineTo(px, py)
        }
        resonancePath.close()

        if (skillOpened) {
            val newTick = skillTick + tick
            if (newTick >= skillDuration) {
                skillTick = 0
                skillOpened = false
                skillAlpha = 0.2f
            }
            else {
                val skillProgress = newTick / skillDuration.toFloat()
                skillAlpha = 1.2f * skillProgress * (1 - skillProgress) + 0.2f
                skillTick = newTick
            }
        }

        updateDirty()
    }

    override fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) {
        backgroundSize = viewportSize
        characterBounds = Rect(viewportSize.center, viewportSize.minDimension / 3.5f)
    }

    override fun Drawer.onDraw() {
        characterImage?.let { cv ->
            clip(resonancePath) {
                circle(Colors.Black, characterBounds.center, characterBounds.width / 2f)
                image(cv, characterBounds, alpha = skillAlpha)
            }
        }
        path(path = resonancePath, color = resonanceColor, style = Stroke(width = 20f), alpha = skillAlpha)
    }
}