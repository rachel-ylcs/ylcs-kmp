package love.yinlin.ui.component.container

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import love.yinlin.common.Colors
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.game.info.PaintPath
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.rememberState
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.node.condition

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

@Stable
class PaintCanvasState(basePaths: List<PaintPath> = emptyList()) {
    companion object {
        val defaultWidth = 300.dp
        val defaultHeight = 450.dp
        val defaultColor = Colors.Black
        val defaultBackground = Colors.White
        val colors = arrayOf(
            Colors.Black, Colors.White, Colors.Gray4, Colors.Red4,
            Colors.Orange4, Colors.Yellow4, Colors.Green4, Colors.Cyan4,
            Colors.Blue4, Colors.Purple4, Colors.Steel4, Colors.Pink4
        )
        val widths = arrayOf(
            1f to Icons.Outlined._1k, 2f to Icons.Outlined._2k, 3f to Icons.Outlined._3k,
            4f to Icons.Outlined._4k, 5f to Icons.Outlined._5k, 6f to Icons.Outlined._6k,
        )
    }

    val paths = basePaths.toMutableStateList()
    var color by mutableStateOf(defaultColor)
    var width by mutableFloatStateOf(1f)
}

@Composable
private fun PaintCanvasTool1(
    state: PaintCanvasState,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        PaintCanvasState.colors.forEach { color ->
            ClickIcon(
                icon = Icons.Outlined.Brush,
                color = color,
                onClick = { state.color = color },
                modifier = Modifier.condition(state.color == color) {
                    border(ThemeValue.Border.Small, MaterialTheme.colorScheme.primary, CircleShape)
                }
            )
        }
    }
}

@Composable
private fun PaintCanvasTool2(
    state: PaintCanvasState,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        val canRemove by rememberDerivedState { state.paths.isNotEmpty() }
        ClickIcon(
            icon = Icons.AutoMirrored.Outlined.Undo,
            enabled = canRemove,
            onClick = { state.paths.removeLastOrNull() }
        )
        ClickIcon(
            icon = Icons.Outlined.Delete,
            enabled = canRemove,
            onClick = { state.paths.clear() }
        )
        PaintCanvasState.widths.forEach { (width, icon) ->
            ClickIcon(
                icon = icon,
                onClick = { state.width = width },
                modifier = Modifier.condition(state.width == width) {
                    border(ThemeValue.Border.Small, MaterialTheme.colorScheme.primary, CircleShape)
                }
            )
        }
    }
}

@Composable
private fun PaintCanvasView(
    state: PaintCanvasState,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val currentPath = remember { mutableStateListOf<Long>() }
    var currentOffset: Offset? by rememberState { null }

    Canvas(modifier = modifier
        .clipToBounds()
        .background(PaintCanvasState.defaultBackground)
        .pointerInput(enabled, state) {
            if (enabled) detectDragGestures(
                onDragStart = { currentPath += packFloats(it.x, it.y) },
                onDragEnd = {
                    val distinctPath = when (currentPath.size) {
                        in 0 .. 1 -> null
                        2 -> listOf(currentPath[0], currentPath[1])
                        else -> {
                            val last = currentPath.last()
                            var index = currentPath.size - 2
                            while (index >= 0 && currentPath[index] == last) index--
                            currentPath.take(index + 2)
                        }
                    }
                    distinctPath?.let { state.paths += PaintPath(it, state.width, state.color.toArgb()) }
                    currentPath.clear()
                },
                onDrag = { v, _ -> currentOffset = v.position }
            )
        }.pointerInput(enabled, state) {
            if (enabled) detectTapGestures {
                state.paths += PaintPath(listOf(
                    packFloats(it.x, it.y),
                    packFloats(it.x, it.y)
                ), state.width, state.color.toArgb())
            }
        }
    ) {
        state.paths.fastForEach { (paths, width, color) ->
            drawPaintPath(paths, width, Color(color))
        }
        if (enabled) {
            currentOffset?.let { offset ->
                if (currentPath.isNotEmpty()) {
                    currentPath += packFloats(offset.x, offset.y)
                    drawPaintPath(currentPath, state.width, state.color)
                }
            }
        }
    }
}

@Composable
fun PaintCanvas(
    state: PaintCanvasState,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Column(modifier = Modifier.width(PaintCanvasState.defaultWidth)) {
            if (enabled) {
                PaintCanvasTool1(state = state, modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()))
                PaintCanvasTool2(state = state, modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()))
            }
            PaintCanvasView(
                state = state,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(PaintCanvasState.defaultHeight)
            )
        }
    }
}