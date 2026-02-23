package love.yinlin.compose.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.BackgroundContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.condition
import love.yinlin.data.rachel.game.info.PaintPath

private fun DrawScope.drawPaintPath(paths: List<Long>, ratio: Float, width: Float, color: Color) {
    val path = Path().apply {
        paths.firstOrNull()?.let { first ->
            var x = unpackFloat1(first)
            var y = unpackFloat2(first)
            moveTo(x * ratio, y * ratio)
            for (i in 1 ..< paths.size - 1) {
                x = unpackFloat1(paths[i])
                y = unpackFloat2(paths[i])
                lineTo(x * ratio, y * ratio)
            }
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width * ratio, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@Stable
class PaintCanvasState(basePaths: List<PaintPath> = emptyList()) {
    companion object {
        val defaultColor = Colors.Black
        val defaultBackground = Colors.White
        val colors1 = arrayOf(
            Colors.Black, Colors.White, Colors.Gray4, Colors.Steel4,
            Colors.Pink4
        )
        val colors2 = arrayOf(
            Colors.Red4, Colors.Orange4, Colors.Yellow4, Colors.Green4,
            Colors.Cyan4, Colors.Blue4, Colors.Purple4
        )
        val widths = arrayOf(
            1f to Icons2.k1, 3f to Icons2.k2, 5f to Icons2.k3,
            7f to Icons2.k4, 9f to Icons2.k5, 11f to Icons2.k6,
            13f to Icons2.k7,
        )
    }

    val paths = basePaths.toMutableStateList()
    var color by mutableStateOf(defaultColor)
    var width by mutableFloatStateOf(1f)
}

@Composable
private inline fun PaintCanvasTool(content: @Composable RowScope.() -> Unit) {
    ActionScope.Left.Container(
        modifier = Modifier.fillMaxWidth().background(Theme.color.backgroundVariant).padding(Theme.padding.value).horizontalScroll(rememberScrollState()),
        content = content
    )
}

@Composable
private fun PaintCanvasView(
    state: PaintCanvasState,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val currentPath = remember { mutableStateListOf<Long>() }
        var currentOffset: Offset? by rememberState { null }

        Canvas(modifier = Modifier
            .matchParentSize()
            .clipToBounds()
            .background(PaintCanvasState.defaultBackground)
            .pointerInput(enabled, state) {
                if (enabled) {
                    detectDragGestures(
                        onDragStart = {
                            val ratio = size.width / 360f * density
                            currentPath += packFloats(it.x / ratio, it.y / ratio)
                        },
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
                }
            }.pointerInput(enabled, state) {
                if (enabled) {
                    detectTapGestures(
                        onTap = {
                            val ratio = size.width / 360f * density
                            state.paths += PaintPath(listOf(
                                packFloats(it.x / ratio, it.y / ratio),
                                packFloats(it.x / ratio, it.y / ratio)
                            ), state.width, state.color.toArgb())
                        }
                    )
                }
            }
        ) {
            val ratio = size.width / 360f * density
            state.paths.fastForEach { (paths, width, color) ->
                drawPaintPath(paths, ratio, width, Color(color))
            }
            if (enabled) {
                currentOffset?.let { offset ->
                    if (currentPath.isNotEmpty()) {
                        currentPath += packFloats(offset.x / ratio, offset.y / ratio)
                        drawPaintPath(currentPath, ratio, state.width, state.color)
                    }
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
    Column(modifier = modifier.width(IntrinsicSize.Min).border(Theme.border.v6, Theme.color.primary).clipToBounds()) {
        BackgroundContainer {
            if (enabled) {
                PaintCanvasTool {
                    val canRemove by rememberDerivedState { state.paths.isNotEmpty() }

                    Icon(icon = Icons.Undo, enabled = canRemove, onClick = state.paths::removeLastOrNull)
                    Icon(icon = Icons.Delete, enabled = canRemove, onClick = state.paths::clear)
                    PaintCanvasState.colors1.forEach { color ->
                        Icon(
                            icon = Icons.Brush,
                            color = color,
                            onClick = { state.color = color },
                            modifier = Modifier.condition(state.color == color) {
                                border(Theme.border.v5, Theme.color.primary)
                            }
                        )
                    }
                }
                PaintCanvasTool {
                    PaintCanvasState.colors2.forEach { color ->
                        Icon(
                            icon = Icons.Brush,
                            color = color,
                            onClick = { state.color = color },
                            modifier = Modifier.condition(state.color == color) {
                                border(Theme.border.v5, Theme.color.primary)
                            }
                        )
                    }
                }
                PaintCanvasTool {
                    PaintCanvasState.widths.forEach { (width, icon) ->
                        Icon(
                            icon = icon,
                            onClick = { state.width = width },
                            modifier = Modifier.condition(state.width == width) {
                                border(Theme.border.v5, Theme.color.primary)
                            }
                        )
                    }
                }
            }
            PaintCanvasView(
                state = state,
                enabled = enabled,
                modifier = Modifier.widthIn(min = Theme.size.cell1).fillMaxWidth().aspectRatio(0.666667f)
            )
        }
    }
}