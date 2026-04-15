package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import love.yinlin.compose.Colors
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Visible
import kotlin.math.sin
import kotlin.random.Random

object BackgroundConstellation : Visible(), Dynamic {
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
            alpha = 0.15f
        )

        val position: Offset get() = Offset(x, y)
    }

    enum class LinkState { IDLE, LINKING, FADING }

    var backgroundSize = Size.Zero
    var linkState = LinkState.IDLE
    var boxes = emptyList<BoxNode>()
    val pathNodes = mutableListOf<BoxNode>()
    var currentTarget: BoxNode? = null
    var linkProgress = 0f
    var fadeAlpha = 1f
    val smallStroke = Stroke(5f, cap = StrokeCap.Round)
    val largeStroke = Stroke(8f, cap = StrokeCap.Round)

    override fun onUpdate(tick: Int) {
        val (w, h) = backgroundSize
        if (w == 0f || h == 0f || boxes.isEmpty()) return

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

    override val layerOrder: Int = 0

    override fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) {
        val (w, h) = viewportSize
        backgroundSize = viewportSize
        if (boxes.isEmpty()) boxes = List(20) { BoxNode(x = Random.nextFloat() * w, y = Random.nextFloat() * h) }
    }

    override fun Drawer.onDraw() {
        val stableLineAlpha = if (linkState == LinkState.FADING) fadeAlpha * 0.1f else 0.1f

        if (pathNodes.size > 1) {
            for (i in 0 ..< pathNodes.size - 1) {
                val start = pathNodes[i]
                val end = pathNodes[i + 1]
                line(
                    color = Colors.White.copy(alpha = stableLineAlpha),
                    start = start.position,
                    end = end.position,
                    style = smallStroke
                )
            }
        }

        if (linkState == LinkState.FADING && pathNodes.size > 1) {
            val start = pathNodes.last()
            val end = pathNodes[0]
            line(
                color = Colors.White.copy(alpha = stableLineAlpha),
                start = start.position,
                end = end.position,
                style = smallStroke
            )
        }

        val target = currentTarget
        if (linkState == LinkState.LINKING && target != null && pathNodes.isNotEmpty()) {
            val startPosition = pathNodes.last().position
            val endPosition = startPosition + (target.position - startPosition) * linkProgress

            line(
                color = Colors.White.copy(alpha = 0.15f),
                start = startPosition,
                end = endPosition,
                style = largeStroke
            )
        }

        boxes.fastForEach { box ->
            val actualSize = 100f * box.currentScale
            val halfSize = actualSize / 2f

            rotate(box.angle, box.position) {
                rect(
                    color = box.color,
                    position = Offset(box.x - halfSize, box.y - halfSize),
                    size = Size(actualSize, actualSize),
                    style = largeStroke
                )
                if (box.currentScale > 1.15f) {
                    rect(
                        color = box.color.copy(alpha = box.color.alpha * 0.5f),
                        position = Offset(box.x - halfSize * 1.25f, box.y - halfSize * 1.25f),
                        size = Size(actualSize * 1.25f, actualSize * 1.25f),
                        style = smallStroke
                    )
                }
            }
        }
    }
}