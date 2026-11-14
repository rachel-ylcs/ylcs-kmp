package love.yinlin.compose.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.text.SimpleClipText
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TimeWheelVisibleRows = 5

@Composable
private fun TimePickerWheel(
    state: PagerState,
    range: Int,
    rowHeight: Dp,
    textLayouts: TimeWheelTextLayouts,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val normalColor = LocalColor.current
    val fadedColor = LocalColorVariant.current.copy(alpha = 0.55f)
    val selectedColor = Theme.color.onBackground
    val wheelHeight = rowHeight * TimeWheelVisibleRows

    Box(modifier = modifier.height(wheelHeight).clipToBounds().pointerIcon(PointerIcon.Hand).focusRequester(focusRequester).focusable()) {
        VerticalPager(
            state = state,
            modifier = Modifier.matchParentSize().pointerInput(state, scope) {
                detectTapGestures { point ->
                    focusRequester.requestFocus()
                    val pageSize = state.layoutInfo.pageSize
                    if (pageSize > 0) {
                        val offset = ((point.y - size.height / 2f) / pageSize + state.currentPageOffsetFraction).roundToInt()
                        val target = (state.currentPage + offset).coerceIn(0, state.pageCount - 1)
                        scope.launch { state.animateScrollToPage(target) }
                    }
                }
            },
            pageSize = PageSize.Fixed(rowHeight),
            contentPadding = PaddingValues(vertical = wheelHeight / 2),
            overscrollEffect = null
        ) {}

        Box(modifier = Modifier.matchParentSize().drawBehind {
            val page = state.currentPage
            val offset = state.currentPageOffsetFraction
            val pageSize = state.layoutInfo.pageSize
            val centerY = size.height / 2f
            val radius = TimeWheelVisibleRows / 2 + 1

            for (delta in -radius .. radius) {
                val row = page + delta
                if (row in 0 ..< state.pageCount) {
                    val value = TimePickerState.wheelValue(row, range)
                    val layout = textLayouts[value]
                    val y = centerY + (delta - offset) * pageSize - layout.size.height / 2f

                    if (y < size.height && y + layout.size.height > 0f) {
                        val color = when (abs(delta)) {
                            0 -> selectedColor
                            1 -> normalColor
                            else -> fadedColor
                        }
                        val x = (size.width - layout.size.width) / 2f

                        drawText(layout, color = color, topLeft = Offset(x, y))
                    }
                }
            }
        })
    }
}

@Composable
fun TimePicker(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    style: TextStyle = Theme.typography.v7
) {
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val textLayouts = remember(measurer, style) { TimeWheelTextLayouts(measurer, style) }

    val textHeight = with(LocalDensity.current) { textLayouts[0].size.height.toDp() }
    val rowHeight = textHeight * 1.6f
    val columnWidth = textHeight * 3f
    val headerGap = textHeight * 0.3f
    val wheelWidth = columnWidth * 3

    Column(
        modifier = modifier.width(wheelWidth),
        verticalArrangement = Arrangement.spacedBy(headerGap)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
            for (unit in "时分秒") {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    SimpleClipText(text = unit.toString(), style = style, color = LocalColorVariant.current)
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(rowHeight * TimeWheelVisibleRows)) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .align(Alignment.Center)
                .background(Theme.color.backgroundVariant, Theme.shape.v5)
                .zIndex(1f)
            )

            Row(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                TimePickerWheel(state.hours, TimePickerState.HOURS, rowHeight, textLayouts, Modifier.weight(1f))
                TimePickerWheel(state.minutes, TimePickerState.MINUTES, rowHeight, textLayouts, Modifier.weight(1f))
                TimePickerWheel(state.seconds, TimePickerState.SECONDS, rowHeight, textLayouts, Modifier.weight(1f))
            }
        }
    }
}
