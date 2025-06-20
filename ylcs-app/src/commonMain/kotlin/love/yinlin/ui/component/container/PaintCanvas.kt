package love.yinlin.ui.component.container

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlinx.serialization.Serializable
import love.yinlin.common.Colors
import love.yinlin.extension.rememberState

@Stable
@Serializable
data class PaintPath(
    val paths: List<Long>,
    val width: Float,
    val color: ULong
)

private fun DrawScope.drawPaintPath(paths: List<Long>, width: Float, color: Color) {
    val path = Path().apply {
        paths.firstOrNull()?.let { first ->
            var x = unpackFloat1(first)
            var y = unpackFloat2(first)
            moveTo(x, y)
            for (i in 1 ..< paths.size - 1) {
                x = unpackFloat1(paths[i])
                y = unpackFloat2(paths[i])
                lineTo(x, y)
            }
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@Composable
fun PaintCanvas(
    items: List<PaintPath>,
    color: Color,
    width: Float,
    onPathAdded: (PaintPath) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val currentPath = remember { mutableStateListOf<Long>() }
        var currentOffset: Offset? by rememberState { null }

        Canvas(modifier = Modifier.matchParentSize()
            .background(Colors.White)
            .pointerInput(color, width, onPathAdded) {
                detectDragGestures(
                    onDragStart = { currentPath += packFloats(it.x, it.y) },
                    onDragEnd = {
                        if (currentPath.isNotEmpty()) {
                            onPathAdded(PaintPath(currentPath.toList(), width, color.value))
                            currentPath.clear()
                        }
                    },
                    onDrag = { v, _ -> currentOffset = v.position }
                )
            }.pointerInput(color, width, onPathAdded) {
                detectTapGestures {
                    onPathAdded(PaintPath(listOf(
                        packFloats(it.x, it.y),
                        packFloats(it.x, it.y)
                    ), width, color.value))
                }
            }
        ) {
            items.fastForEach { (paths, width, color) ->
                drawPaintPath(paths, width, Color(color))
            }
            currentOffset?.let { offset ->
                if (currentPath.isNotEmpty()) {
                    currentPath += packFloats(offset.x, offset.y)
                    drawPaintPath(currentPath, width, color)
                }
            }
        }
    }
}