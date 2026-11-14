package love.yinlin.compose.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.measureTextHeight

// 24 和 60 的公倍数，确保初始页对应 00，并给滚动留出双向页数。
private const val TimeWheelCenterPage = 1_000_000_080

@Composable
private fun TimePickerWheel(
    value: Int,
    range: Int,
    unit: String,
    columnWidth: Dp,
    rowHeight: Dp,
    style: TextStyle,
    onValueChanged: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val state = rememberPagerState(initialPage = TimeWheelCenterPage + value) { Int.MAX_VALUE }
    val updatedValue by rememberUpdatedState(value)
    val updatedOnValueChanged by rememberUpdatedState(onValueChanged)
    val normalColor = LocalColor.current
    val fadedColor = LocalColorVariant.current
    val selectedColor = Theme.color.onBackground

    // 外部修改 time 时才同步滚轮位置；内部选择回写不会重置分页状态。
    LaunchedEffect(value) {
        if (!state.isScrollInProgress && (state.settledPage - TimeWheelCenterPage).mod(range) != value) {
            state.scrollToPage(TimeWheelCenterPage + value)
        }
    }

    // 只在滚动停稳后通知调用方；初次显示不触发回调。
    LaunchedEffect(state) {
        snapshotFlow { state.settledPage }.drop(1).collect { page ->
            val selected = (page - TimeWheelCenterPage).mod(range)
            if (selected != updatedValue) updatedOnValueChanged(selected)
        }
    }

    VerticalPager(
        state = state,
        modifier = Modifier.size(columnWidth, rowHeight * 5).semantics {
            contentDescription = "$unit，当前为${(state.currentPage - TimeWheelCenterPage).mod(range)}"
        },
        pageSize = PageSize.Fixed(rowHeight),
        contentPadding = PaddingValues(vertical = rowHeight * 2),
        beyondViewportPageCount = 1
    ) { page ->
        val distance = kotlin.math.abs(page - state.currentPage)
        val color = when (distance) {
            0 -> selectedColor
            1 -> normalColor
            else -> fadedColor.copy(alpha = 0.55f)
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(rowHeight).clickable {
                scope.launch { state.animateScrollToPage(page) }
            },
            contentAlignment = Alignment.Center
        ) {
            SimpleClipText(
                text = (page - TimeWheelCenterPage).mod(range).toString().padStart(2, '0'),
                color = color,
                style = style,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 独立的 24 小时制时分秒滚轮组件，不包含输入框、弹窗、确认或取消操作。
 *
 * 三列滚轮可独立滑动或点击数字选择；滚动停稳且选中值改变时调用 [onTimeChanged]。
 * 不含固定 dp 尺寸，宽度、行高及间距由 [style] 对应的文本高度推导。
 * 弹窗容器、确认操作及输入框联动由调用方自由组合。
 */
@Composable
fun TimePicker(
    time: LocalTime,
    onTimeChanged: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = Theme.typography.v7
) {
    val textHeight = measureTextHeight("00", style)
    val rowHeight = textHeight * 1.6f
    val columnWidth = textHeight * 3.2f
    val headerGap = textHeight * 0.3f
    val contentWidth = columnWidth * 3
    val updatedTime by rememberUpdatedState(time)
    val updatedOnTimeChanged by rememberUpdatedState(onTimeChanged)

    Column(
        modifier = modifier.width(contentWidth),
        verticalArrangement = Arrangement.spacedBy(headerGap)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            "时分秒".forEach { unit ->
                Box(modifier = Modifier.size(columnWidth, rowHeight), contentAlignment = Alignment.Center) {
                    SimpleClipText(unit.toString(), style = style, color = LocalColorVariant.current)
                }
            }
        }

        Box(modifier = Modifier.size(contentWidth, rowHeight * 5)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(rowHeight).align(Alignment.Center)
                    .background(Theme.color.backgroundVariant, Theme.shape.v5)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                TimePickerWheel(time.hour, 24, "时", columnWidth, rowHeight, style) { hour ->
                    val current = updatedTime
                    if (hour != current.hour) updatedOnTimeChanged(LocalTime(hour, current.minute, current.second, current.nanosecond))
                }
                TimePickerWheel(time.minute, 60, "分", columnWidth, rowHeight, style) { minute ->
                    val current = updatedTime
                    if (minute != current.minute) updatedOnTimeChanged(LocalTime(current.hour, minute, current.second, current.nanosecond))
                }
                TimePickerWheel(time.second, 60, "秒", columnWidth, rowHeight, style) { second ->
                    val current = updatedTime
                    if (second != current.second) updatedOnTimeChanged(LocalTime(current.hour, current.minute, second, current.nanosecond))
                }
            }
        }
    }
}