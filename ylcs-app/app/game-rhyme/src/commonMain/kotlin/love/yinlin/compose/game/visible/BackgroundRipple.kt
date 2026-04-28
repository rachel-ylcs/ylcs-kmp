package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.fastCoerceIn
import love.yinlin.app.global.resources.Res
import love.yinlin.app.global.resources.xwwk
import love.yinlin.compose.Colors
import love.yinlin.compose.extension.translate
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.drawer.TextGraph
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

    var characterRadius: Float = 0f
    var characterBounds: Rect = Rect.Zero

    var skillTick: Int = 0
    var skillAlpha: Float = 0.2f
    var skillOpened: Boolean = false
    val skillDuration: Int = 300

    var skillShowText: String? = null
    var skillShowTextGraph: TextGraph? = null

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
        characterRadius = viewportSize.minDimension / 3.5f
        characterBounds = Rect(viewportSize.center, characterRadius)

        val newText = character.showText
        if (skillShowText != newText) {
            skillShowText = newText
            skillShowTextGraph = newText?.let { measureText(it, Res.font.xwwk, FontWeight.Bold) }
        }
    }

    override fun Drawer.onDraw() {
        val characterCenter = characterBounds.center

        clip(resonancePath) {
            characterImage?.let { cv ->
                circle(Colors.Black, characterCenter, characterRadius)
                image(cv, characterBounds, alpha = skillAlpha)
            }

            skillShowTextGraph?.let { graph ->
                val h = characterRadius / 8f
                val w = graph.width(h)
                roundRect(Colors.Black, h, characterCenter.translate(x = -w, y = h * 4), Size(w * 2, h), alpha = 0.75f)
                text(graph, characterCenter.translate(x = -w / 2, y = h * 4), Size(w, h), Colors.White.copy(alpha = skillAlpha))
            }
        }

        path(path = resonancePath, color = resonanceColor, style = Stroke(width = 20f), alpha = skillAlpha)
    }
}