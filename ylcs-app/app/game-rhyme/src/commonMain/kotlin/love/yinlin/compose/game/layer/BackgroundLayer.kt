package love.yinlin.compose.game.layer

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import love.yinlin.compose.Colors
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.LayerType
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Layer
import love.yinlin.compose.game.traits.Visible
import love.yinlin.compose.game.viewport.Camera
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Stable
open class BackgroundLayer : Layer(layerOrder = 0, layerType = LayerType.Absolute) {
    private class Wave(val camera: Camera) : Visible(), Dynamic {
        var wavePhase1 = 0f
        var wavePhase2 = 0f
        var waveAmplitudeTime = 0f
        val frequency = 0.001f
        val step = 20f
        val wavePath1 = Path()
        val wavePath2 = Path()
        val waveColor1 = Colors(0xFF00E5FF).copy(alpha = 0.05f)
        val waveColor2 = Colors(0xFFFF00FF).copy(alpha = 0.05f)

        override fun onUpdate(tick: Int) {
            size = camera.viewportSize

            wavePhase1 -= tick * 0.003f
            wavePhase2 -= tick * 0.002f
            waveAmplitudeTime += tick * 0.001f
            wavePath1.reset()
            wavePath2.reset()
            val (w, h) = size
            val hy = h / 2
            val dynamicAmplitude = hy * (0.85f + 0.15f * sin(waveAmplitudeTime)) / 4
            var x = 0f
            wavePath1.moveTo(0f, hy + sin(wavePhase1) * dynamicAmplitude)
            wavePath2.moveTo(0f, hy + sin(wavePhase2 + 3.141592f) * dynamicAmplitude)
            while (x <= w) {
                val y1 = hy + sin(x * frequency + wavePhase1) * dynamicAmplitude
                val y2 = hy + sin(x * frequency * 1.2f + wavePhase2) * dynamicAmplitude * 0.8f
                wavePath1.lineTo(x, y1)
                wavePath2.lineTo(x, y2)
                x += step
            }
            wavePath1.lineTo(w, hy + sin(w * frequency + wavePhase1) * dynamicAmplitude)
            wavePath2.lineTo(w, hy + sin(w * frequency * 1.2f + wavePhase2) * dynamicAmplitude * 0.8f)

            updateDirty()
        }

        override fun Drawer.onDraw() {
            path(path = wavePath1, color = waveColor1, style = Stroke(width = 16f))
            path(path = wavePath2, color = waveColor2, style = Stroke(width = 16f))
        }
    }

    private class Ripple(val camera: Camera) : Visible(), Dynamic {
        var resonanceTime = 0f
        var resonanceUpdateTimer = 0f
        val resonanceUpdateInterval = 100f
        val resonancePath = Path()
        val resonanceColor = Colors(0xFF7C4DFF).copy(alpha = 0.1f)
        var resonanceTargetScale = 1f
        var resonanceCurrentScale = 1f
        val edgeNoises = FloatArray(50)

        override fun onUpdate(tick: Int) {
            size = camera.viewportSize

            resonanceTime += tick * 0.01f
            resonanceUpdateTimer += tick
            resonanceCurrentScale += (resonanceTargetScale - resonanceCurrentScale) * (tick * 0.01f).coerceIn(0f, 1f)

            if (resonanceUpdateTimer >= resonanceUpdateInterval) {
                resonanceUpdateTimer = 0f
                resonanceTargetScale = 0.9f + Random.nextFloat() * 0.3f
                repeat(edgeNoises.size) { i ->
                    edgeNoises[i] = sin(resonanceTime * 5 + i) * 10 + Random.nextFloat() * 8f
                }
            }

            val (centerX, centerY) = center
            val currentBaseRadius = size.minDimension * resonanceCurrentScale / 4
            resonancePath.reset()
            repeat(edgeNoises.size) { i ->
                val angleRad = i * 6.283184f / edgeNoises.size
                val r = currentBaseRadius + edgeNoises[i]
                val px = centerX + r * cos(angleRad)
                val py = centerY + r * sin(angleRad)
                if (i == 0) resonancePath.moveTo(px, py) else resonancePath.lineTo(px, py)
            }
            resonancePath.close()

            updateDirty()
        }

        override fun Drawer.onDraw() {
            path(path = resonancePath, color = resonanceColor, style = Stroke(width = 20f))
        }
    }

    private class Constellation(val camera: Camera) : Visible(), Dynamic {
        class BoxNode(var x: Float, var y: Float) {
            var scaleTime = Random.nextFloat() * 100f
            var scaleSpeed = Random.nextFloat() * 0.003f + 0.001f
            var currentScale = 1f

            var angle = Random.nextFloat() * 360f
            var rotateSpeed = (Random.nextFloat() * 0.05f + 0.01f) * (if (Random.nextBoolean()) 1 else -1)

            var dx = (Random.nextFloat() - 0.5f) * 0.1f
            var dy = (Random.nextFloat() - 0.5f) * 0.1f

            val color = Colors(
                red = Random.nextFloat() * 0.5f + 0.5f,
                green = Random.nextFloat() * 0.5f + 0.5f,
                blue = Random.nextFloat() * 0.5f + 0.5f,
                alpha = 0.1f
            )
        }

        enum class LinkState { IDLE, LINKING, FADING }

        private var linkState = LinkState.IDLE
        private var boxes = emptyList<BoxNode>()
        private val pathNodes = mutableListOf<BoxNode>()
        private var currentTarget: BoxNode? = null
        private var linkProgress = 0f
        private var fadeAlpha = 1f

        override fun onUpdate(tick: Int) {
            size = camera.viewportSize
            val (w, h) = size

            if (boxes.isEmpty()) boxes = List(20) { BoxNode(x = Random.nextFloat() * w, y = Random.nextFloat() * h) }

            boxes.fastForEach { box ->
                box.x += box.dx * tick
                box.y += box.dy * tick
                if (box.x < 0f) {
                    box.x = 0f
                    box.dx *= -1
                }
                if (box.x > w) {
                    box.x = w
                    box.dx *= -1
                }
                if (box.y < 0f) {
                    box.y = 0f
                    box.dy *= -1
                }
                if (box.y > h) {
                    box.y = h
                    box.dy *= -1
                }
                box.angle += box.rotateSpeed * tick
                box.scaleTime += tick * box.scaleSpeed
                box.currentScale = 1.125f + 0.125f * sin(box.scaleTime)
            }

            when (linkState) {
                LinkState.IDLE -> {
                    pathNodes.clear()
                    if (boxes.isNotEmpty()) {
                        pathNodes += boxes.random()
                        linkState = LinkState.LINKING
                    }
                }
                LinkState.LINKING -> {
                    val target = currentTarget
                    if (target == null) {
                        val unvisited = boxes.fastFilter { it !in pathNodes }
                        currentTarget = if (unvisited.isNotEmpty()) unvisited.random() else pathNodes[0]
                        linkProgress = 0f
                    } else {
                        linkProgress += tick * 0.0005f
                        if (linkProgress >= 1f) {
                            if (target == pathNodes[0]) {
                                linkState = LinkState.FADING
                                fadeAlpha = 1f
                            } else pathNodes += target
                            currentTarget = null
                        }
                    }
                }
                LinkState.FADING -> {
                    fadeAlpha -= tick * 0.001f
                    if (fadeAlpha <= 0f) linkState = LinkState.IDLE
                }
            }

            updateDirty()
        }

        override fun Drawer.onDraw() {
            val stableLineAlpha = if (linkState == LinkState.FADING) fadeAlpha * 0.05f else 0.05f
            if (pathNodes.size > 1) {
                for (i in 0 ..< pathNodes.size - 1) {
                    val start = pathNodes[i]
                    val end = pathNodes[i + 1]
                    line(
                        color = Colors.White.copy(alpha = stableLineAlpha),
                        start = Offset(start.x, start.y),
                        end = Offset(end.x, end.y),
                        style = Stroke(3f)
                    )
                }
            }

            if (linkState == LinkState.FADING && pathNodes.size > 1) {
                val start = pathNodes.last()
                val end = pathNodes[0]
                line(
                    color = Colors.White.copy(alpha = stableLineAlpha),
                    start = Offset(start.x, start.y),
                    end = Offset(end.x, end.y),
                    style = Stroke(3f)
                )
            }

            val target = currentTarget
            if (linkState == LinkState.LINKING && target != null && pathNodes.isNotEmpty()) {
                val start = pathNodes.last()
                val endX = start.x + (target.x - start.x) * linkProgress
                val endY = start.y + (target.y - start.y) * linkProgress

                line(
                    color = Colors.White.copy(alpha = 0.1f),
                    start = Offset(start.x, start.y),
                    end = Offset(endX, endY),
                    style = Stroke(5f)
                )
            }

            boxes.fastForEach { box ->
                val actualSize = 100f * box.currentScale
                val halfSize = actualSize / 2f

                rotate(box.angle, Offset(box.x, box.y)) {
                    rect(
                        color = box.color,
                        position = Offset(box.x - halfSize, box.y - halfSize),
                        size = Size(actualSize, actualSize),
                        style = Stroke(width = 5f)
                    )
                    if (box.currentScale > 1.15f) {
                        rect(
                            color = box.color.copy(alpha = box.color.alpha * 0.5f),
                            position = Offset(box.x - halfSize * 1.25f, box.y - halfSize * 1.25f),
                            size = Size(actualSize * 1.25f, actualSize * 1.25f),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }
    }

    override fun onLayerAttached(scene: ScenePlugin) {
        this += Wave(scene.camera)
        this += Ripple(scene.camera)
        this += Constellation(scene.camera)
    }
}