package love.yinlin.compose.ui.widget

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlinx.coroutines.flow.collectLatest
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberIntState
import love.yinlin.compose.extension.rememberOffsetState
import love.yinlin.extension.cleaning
import kotlin.math.roundToInt

@Stable
class HexViewState(private val data: ByteArray) {
    val size: Int get() = data.size

    internal var selectionAnchor by mutableIntStateOf(-1)
        private set

    var selectedIndex by mutableIntStateOf(-1)
        private set

    val selectedRange: IntRange get() = if (selectedIndex < 0) IntRange.EMPTY else minOf(selectionAnchor, selectedIndex) .. maxOf(selectionAnchor, selectedIndex)

    operator fun get(index: Int): Byte = data[index]

    fun select(index: Int, endIndex: Int = index) {
        require(index in data.indices && endIndex in data.indices) { "HexView selection is out of bounds" }
        selectionAnchor = index
        selectedIndex = endIndex
    }

    internal fun select(index: Int, extend: Boolean) {
        if (extend && selectedIndex == index) return
        select(if (extend && selectionAnchor >= 0) selectionAnchor else index, index)
    }

    fun clearSelection() {
        selectionAnchor = -1
        selectedIndex = -1
    }
}

private val HexViewByteTexts = Array(256) { it.toString(16).uppercase().padStart(2, '0') }
private val HexViewAsciiTexts = Array(256) { if (it in 0x20 .. 0x7E) it.toChar().toString() else "." }
private const val HexViewGroupSize = 8

@Stable
private data class HexViewMetrics(
    val columns: Int,
    val width: Int,
    val rowHeight: Int,
    val padding: Float,
    val offsetWidth: Float,
    val hexCellWidth: Float,
    val asciiCellWidth: Float,
    val groupGap: Float,
    val asciiGap: Float,
    val showAscii: Boolean,
) {
    val hexStart: Float = padding + offsetWidth
    val hexEnd: Float = hexStart + columns * hexCellWidth + (columns / HexViewGroupSize - 1) * groupGap
    val asciiStart: Float = hexEnd + asciiGap

    fun hexPosition(column: Int): Float = hexStart + column * hexCellWidth + column / HexViewGroupSize * groupGap

    fun columnAt(x: Float, ascii: Boolean, clamp: Boolean): Int? {
        val start = if (ascii) asciiStart else hexStart
        val end = if (ascii) asciiStart + columns * asciiCellWidth else hexEnd
        if (!clamp && x !in start ..< end) return null

        val position = (x - start).coerceAtLeast(0f)
        val column = if (ascii) {
            (position / asciiCellWidth).toInt()
        }
        else {
            val groupWidth = HexViewGroupSize * hexCellWidth + groupGap
            val group = (position / groupWidth).toInt()
            val withinGroup = position - group * groupWidth
            if (!clamp && withinGroup >= HexViewGroupSize * hexCellWidth) return null
            group * HexViewGroupSize + (withinGroup / hexCellWidth).toInt().coerceAtMost(HexViewGroupSize - 1)
        }
        return column.coerceIn(0, columns - 1)
    }

    fun indexAt(position: Offset, listState: LazyListState, size: Int, ascii: Boolean, clamp: Boolean): Int? {
        if (size == 0) return null
        val column = columnAt(position.x, ascii, clamp) ?: return null
        val layout = listState.layoutInfo
        val start = layout.viewportStartOffset.toFloat()
        val end = layout.viewportEndOffset.toFloat()
        if (end <= start || (!clamp && position.y !in start ..< end)) return null
        val y = if (clamp) position.y.coerceIn(start, (end - 1f).coerceAtLeast(start)) else position.y
        val row = layout.visibleItemsInfo.firstOrNull { y >= it.offset && y < it.offset + it.size } ?: return null
        val index = row.index.toLong() * columns + column
        return if (clamp) index.coerceAtMost(size.toLong() - 1).toInt() else index.toInt().takeIf { index < size }
    }
}

private fun DrawScope.drawHexViewText(result: TextLayoutResult, color: Color, x: Float, width: Float, centered: Boolean = true) {
    drawText(
        textLayoutResult = result,
        color = color,
        topLeft = Offset(x + if (centered) (width - result.size.width) / 2f else 0f, (size.height - result.size.height) / 2f),
    )
}

@Composable
private fun HexViewHeader(metrics: HexViewMetrics, measurer: TextMeasurer, style: TextStyle) {
    val color = LocalColorVariant.current
    val background = Theme.color.backgroundVariant
    val outline = Theme.color.outline.copy(alpha = 0.25f)
    val borderWidth = with(LocalDensity.current) { Theme.border.v7.toPx() }
    val height = with(LocalDensity.current) { metrics.rowHeight.toDp() }

    Box(modifier = Modifier.fillMaxWidth().height(height).drawWithCache {
        val offsetText = measurer.measure("OFFSET", style, maxLines = 1)
        val labels = Array(metrics.columns) { measurer.measure(it.toString(16).uppercase().padStart(2, '0'), style, maxLines = 1) }
        val asciiText = if (metrics.showAscii) measurer.measure("ASCII", style, maxLines = 1) else null
        onDrawBehind {
            drawRect(background)
            drawHexViewText(offsetText, color, metrics.padding, metrics.offsetWidth, centered = false)
            for (column in labels.indices) drawHexViewText(labels[column], color, metrics.hexPosition(column), metrics.hexCellWidth)
            if (asciiText != null) drawHexViewText(asciiText, color, metrics.asciiStart, metrics.columns * metrics.asciiCellWidth, centered = false)
            drawRect(outline, topLeft = Offset(0f, size.height - borderWidth), size = Size(size.width, borderWidth))
        }
    })
}

@Composable
private fun HexViewRow(
    state: HexViewState,
    row: Int,
    metrics: HexViewMetrics,
    measurer: TextMeasurer,
    style: TextStyle
) {
    val color = LocalColor.current
    val variantColor = LocalColorVariant.current
    val primary = Theme.color.primary
    val selectionColor = primary.copy(alpha = 0.2f)
    val background = if (row % 2 == 0) Theme.color.background else Theme.color.surface
    val borderWidth = with(LocalDensity.current) { Theme.border.v7.toPx() }
    val height = with(LocalDensity.current) { metrics.rowHeight.toDp() }

    Box(modifier = Modifier.fillMaxWidth().height(height).drawWithCache {
        val offset = row * metrics.columns
        val count = minOf(metrics.columns, state.size - offset)
        val offsetText = measurer.measure(offset.toString(16).uppercase().padStart(8, '0'), style, maxLines = 1)
        val hexTexts = Array(count) { measurer.measure(HexViewByteTexts[state[offset + it].toInt() and 0xFF], style, maxLines = 1) }
        val asciiTexts = if (metrics.showAscii) Array(count) {
            measurer.measure(HexViewAsciiTexts[state[offset + it].toInt() and 0xFF], style, maxLines = 1)
        } else null

        onDrawBehind {
            drawRect(background)
            drawHexViewText(offsetText, variantColor, metrics.padding, metrics.offsetWidth, centered = false)

            val selection = state.selectedRange
            val currentIndex = state.selectedIndex
            for (column in 0 until count) {
                val index = offset + column
                val selected = index in selection
                val hexX = metrics.hexPosition(column)
                val asciiX = metrics.asciiStart + column * metrics.asciiCellWidth

                if (selected) {
                    drawRect(selectionColor, Offset(hexX, 0f), Size(metrics.hexCellWidth, size.height))
                    if (asciiTexts != null) drawRect(selectionColor, Offset(asciiX, 0f), Size(metrics.asciiCellWidth, size.height))
                    if (index == currentIndex) {
                        val inset = borderWidth / 2f
                        drawRect(primary, Offset(hexX + inset, inset), Size(metrics.hexCellWidth - borderWidth, size.height - borderWidth), style = Stroke(borderWidth))
                        if (asciiTexts != null) drawRect(primary, Offset(asciiX + inset, inset), Size(metrics.asciiCellWidth - borderWidth, size.height - borderWidth), style = Stroke(borderWidth))
                    }
                }

                drawHexViewText(hexTexts[column], if (selected) primary else color, hexX, metrics.hexCellWidth)
                if (asciiTexts != null) {
                    val value = state[index].toInt() and 0xFF
                    val asciiColor = if (selected) primary else if (value in 0x20 .. 0x7E) color else variantColor
                    drawHexViewText(asciiTexts[column], asciiColor, asciiX, metrics.asciiCellWidth)
                }
            }
        }
    })
}

@Composable
private fun HexViewBody(
    state: HexViewState,
    listState: LazyListState,
    metrics: HexViewMetrics,
    measurer: TextMeasurer,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    var dragPosition by rememberOffsetState(state, metrics) { Offset.Unspecified }
    var dragAscii by rememberFalse(state, metrics)

    LaunchedEffect(state, metrics, listState) {
        snapshotFlow { dragPosition.isSpecified }.collectLatest { dragging ->
            if (dragging) {
                var previousFrame = withFrameNanos { it }
                while (true) {
                    val frame = withFrameNanos { it }
                    if (dragPosition.isUnspecified) break
                    val position = dragPosition
                    val height = listState.layoutInfo.viewportEndOffset.toFloat()
                    val edge = minOf(metrics.rowHeight * 1.5f, height / 2f)
                    if (edge > 0f) {
                        val speed = when {
                            position.y < edge -> ((position.y - edge) / edge).coerceAtLeast(-1f)
                            position.y > height - edge -> ((position.y - height + edge) / edge).coerceAtMost(1f)
                            else -> 0f
                        }
                        val seconds = ((frame - previousFrame) / 1000000000f).coerceAtMost(0.05f)
                        if (speed != 0f) listState.scrollBy(speed * metrics.rowHeight * 12f * seconds)
                        metrics.indexAt(position, listState, state.size, dragAscii, clamp = true)?.let { state.select(it, extend = true) }
                    }
                    previousFrame = frame
                }
            }
        }
    }

    Box(
        modifier = modifier.clipToBounds().pointerInput(state, metrics, listState) {
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                val ascii = metrics.showAscii && down.position.x >= metrics.asciiStart
                val anchor = metrics.indexAt(down.position, listState, state.size, ascii, clamp = false) ?: return@awaitEachGesture

                val press = if (down.type == PointerType.Touch) {
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        val up = currentEvent.changes.firstOrNull { it.id == down.id && it.changedToUp() }
                        if (up != null) {
                            up.consume()
                            state.select(anchor, extend = false)
                        }
                        return@awaitEachGesture
                    }
                    longPress
                } else down

                press.consume()
                state.select(anchor, extend = false)
                dragAscii = ascii
                dragPosition = press.position
                cleaning({ dragPosition = Offset.Unspecified }) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == press.id } ?: break
                        if (change.isConsumed) break
                        change.consume()
                        dragPosition = change.position
                        metrics.indexAt(change.position, listState, state.size, ascii, clamp = true)?.let { state.select(it, extend = true) }
                        if (!change.pressed || (change.type == PointerType.Mouse && !event.buttons.isPrimaryPressed)) break
                    }
                }
            }
        },
        propagateMinConstraints = true
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            val rows = state.size / metrics.columns + if (state.size % metrics.columns == 0) 0 else 1
            items(count = rows, key = { it }) { row ->
                HexViewRow(state, row, metrics, measurer, style)
            }
        }
    }
}

/**
 * 十六进制预览器
 *
 * @param showAscii 是否显示 ASCII 区域，不可打印字节用 `.` 占位。
 */
@Composable
fun HexView(
    state: HexViewState,
    modifier: Modifier = Modifier,
    showAscii: Boolean = true
) {
    val density = LocalDensity.current
    val style = Theme.typography.v8.copy(fontFamily = FontFamily.Monospace)
    val headerStyle = style.copy(fontWeight = FontWeight.SemiBold)
    val measurer = rememberTextMeasurer(cacheSize = 512)
    val listState = remember(state) { LazyListState() }
    val horizontalState = rememberScrollState()
    val padding = Theme.padding.h
    val gap = Theme.padding.g7
    val verticalPadding = Theme.padding.v
    val minCellWidth = Theme.size.input10
    val minRowHeight = Theme.size.input9
    val minWidth = Theme.size.cell1

    BoxWithConstraints(
        modifier = modifier.layout { measurable, constraints ->
            val width = constraints.constrainWidth(minWidth.roundToPx())
            val height = constraints.constrainHeight((width / 2f).roundToInt())
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(width, height) {
                placeable.placeRelative(0, 0)
            }
        }.clipToBounds(),
        propagateMinConstraints = true,
    ) {
        val metrics = remember(constraints.maxWidth, density, style, headerStyle, measurer, showAscii, padding, gap, verticalPadding, minCellWidth, minRowHeight) {
            val byteText = measurer.measure("FF", style, maxLines = 1)
            val addressText = measurer.measure("00000000", style, maxLines = 1)
            val asciiText = measurer.measure("M", style, maxLines = 1)
            val headerText = measurer.measure("OFFSET", headerStyle, maxLines = 1)
            with(density) {
                val paddingPx = padding.roundToPx().coerceAtLeast(0)
                val gapPx = gap.roundToPx().coerceAtLeast(0)
                val offsetWidth = maxOf(addressText.size.width, headerText.size.width) + paddingPx
                val hexWidth = maxOf(byteText.size.width + gapPx * 2, minCellWidth.roundToPx(), 1)
                val asciiWidth = (asciiText.size.width + gapPx).coerceAtLeast(1)
                val asciiGap = if (showAscii) paddingPx else 0
                val fixedWidth = paddingPx * 2 + offsetWidth + asciiGap
                val groupWidth = HexViewGroupSize * (hexWidth + if (showAscii) asciiWidth else 0) + paddingPx
                val availableWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else fixedWidth + groupWidth - paddingPx
                val groups = ((availableWidth - fixedWidth + paddingPx) / groupWidth).coerceAtLeast(1)
                val columns = groups * HexViewGroupSize
                val minimumWidth = fixedWidth + groups * groupWidth - paddingPx
                val width = maxOf(availableWidth, minimumWidth)
                val expandedHexWidth = hexWidth + (width - minimumWidth).toFloat() / columns
                HexViewMetrics(
                    columns = columns,
                    width = width,
                    rowHeight = maxOf(byteText.size.height, addressText.size.height, asciiText.size.height, headerText.size.height, 1)
                        .plus(verticalPadding.roundToPx().coerceAtLeast(0) * 2).coerceAtLeast(minRowHeight.roundToPx()),
                    padding = paddingPx.toFloat(),
                    offsetWidth = offsetWidth.toFloat(),
                    hexCellWidth = expandedHexWidth,
                    asciiCellWidth = asciiWidth.toFloat(),
                    groupGap = paddingPx.toFloat(),
                    asciiGap = asciiGap.toFloat(),
                    showAscii = showAscii,
                )
            }
        }

        var previousColumns by rememberIntState(state) { metrics.columns }
        val resizeAnchor = remember(state, metrics.columns) {
            if (previousColumns == metrics.columns) null else {
                val byteOffset = (listState.firstVisibleItemIndex.toLong() * previousColumns).coerceAtMost((state.size - 1).coerceAtLeast(0).toLong())
                byteOffset to listState.firstVisibleItemScrollOffset
            }
        }
        LaunchedEffect(metrics.columns, state) {
            if (resizeAnchor != null && state.size > 0) {
                listState.scrollToItem((resizeAnchor.first / metrics.columns).toInt(), resizeAnchor.second)
            }
            previousColumns = metrics.columns
        }

        val width = with(density) { metrics.width.toDp() }
        Box(
            modifier = Modifier.horizontalScroll(horizontalState),
            propagateMinConstraints = true
        ) {
            Column(modifier = Modifier.width(width)) {
                HexViewHeader(metrics, measurer, headerStyle)
                HexViewBody(state, listState, metrics, measurer, style, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}
