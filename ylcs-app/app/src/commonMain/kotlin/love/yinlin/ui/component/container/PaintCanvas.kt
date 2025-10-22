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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import love.yinlin.compose.*
import love.yinlin.data.rachel.game.info.PaintPath
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.node.condition

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
            1f to Icons.Outlined._1k, 3f to Icons.Outlined._2k, 5f to Icons.Outlined._3k,
            7f to Icons.Outlined._4k, 9f to Icons.Outlined._5k, 11f to Icons.Outlined._6k,
            13f to Icons.Outlined._7k,
        )
    }

    val paths = basePaths.toMutableStateList()
    var color by mutableStateOf(defaultColor)
    var width by mutableFloatStateOf(1f)
}

@Composable
private inline fun PaintCanvasTool(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(CustomTheme.padding.value)
            .horizontalScroll(rememberScrollState()),
        content = content
    )
}

@Composable
private fun PaintCanvasView(
    state: PaintCanvasState,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val currentPath = remember { mutableStateListOf<Long>() }
        var currentOffset: Offset? by rememberState { null }

        val density = LocalDensity.current.density
        val ratio = remember(maxWidth, density) { maxWidth.value / 360 * density }

        Canvas(modifier = Modifier.fillMaxSize().clipToBounds()
            .background(PaintCanvasState.defaultBackground)
            .pointerInput(maxWidth, ratio, enabled, state) {
                if (enabled) detectDragGestures(
                    onDragStart = { currentPath += packFloats(it.x / ratio, it.y / ratio) },
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
            }.pointerInput(maxWidth, ratio, enabled, state) {
                if (enabled) detectTapGestures {
                    state.paths += PaintPath(listOf(
                        packFloats(it.x / ratio, it.y / ratio),
                        packFloats(it.x / ratio, it.y / ratio)
                    ), state.width, state.color.toArgb())
                }
            }
        ) {
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
    Surface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().border(CustomTheme.border.medium, MaterialTheme.colorScheme.primary)) {
            if (enabled) {
                PaintCanvasTool {
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
                    PaintCanvasState.colors1.forEach { color ->
                        ClickIcon(
                            icon = Icons.Outlined.Brush,
                            color = color,
                            onClick = { state.color = color },
                            modifier = Modifier.condition(state.color == color) {
                                border(CustomTheme.border.small, MaterialTheme.colorScheme.primary, CircleShape)
                            }
                        )
                    }
                }
                PaintCanvasTool {
                    PaintCanvasState.colors2.forEach { color ->
                        ClickIcon(
                            icon = Icons.Outlined.Brush,
                            color = color,
                            onClick = { state.color = color },
                            modifier = Modifier.condition(state.color == color) {
                                border(CustomTheme.border.small, MaterialTheme.colorScheme.primary, CircleShape)
                            }
                        )
                    }
                }
                PaintCanvasTool {
                    PaintCanvasState.widths.forEach { (width, icon) ->
                        ClickIcon(
                            icon = icon,
                            onClick = { state.width = width },
                            modifier = Modifier.condition(state.width == width) {
                                border(CustomTheme.border.small, MaterialTheme.colorScheme.primary, CircleShape)
                            }
                        )
                    }
                }
            }
            PaintCanvasView(
                state = state,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.666667f)
            )
        }
    }
}