package love.yinlin.compose.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.measureTextHeight
import love.yinlin.extension.DateEx

// 24 和 60 的公倍数，初始页映射到 00，留出足够双向滚动空间。
private const val TimeWheelCenterPage = 1_000_000_080

@Composable
private fun TimePickerWheel(
    state: PagerState,
    range: Int,
    unit: String,
    columnWidth: Dp,
    rowHeight: Dp,
    style: TextStyle
) {
    val scope = rememberCoroutineScope()
    val normalColor = LocalColor.current
    val fadedColor = LocalColorVariant.current
    val selectedColor = Theme.color.onBackground

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
 * 24 小时制时分秒独立滚轮，包含右下角“确定”按钮，不含输入框或弹窗。
 *
 * [time] 为空时以首次显示时的 [DateEx.Current] 为起点。滚动或点击数字
 * 仅修改内部候选值；点击“确定”才调用 [onTimeChanged]。外部 [time] 发生
 * 改变时重新定位三个滚轮，组件尺寸随 [style] 对应的文本高度缩放。
 */
@Composable
fun TimePicker(
    time: LocalTime? = null,
    onTimeChanged: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = Theme.typography.v7
) {
    val textHeight = measureTextHeight("00", style)
    val rowHeight = textHeight * 1.6f
    val columnWidth = textHeight * 3.2f
    val headerGap = textHeight * 0.3f
    val contentWidth = columnWidth * 3

    // 空值只在组件初始化时读取当前时间，避免每次重组导致滚轮跳回“现在”。
    val initialTime = remember(time) { time ?: DateEx.Current.time }

    key(time) {
        val hours = rememberPagerState(initialPage = TimeWheelCenterPage + initialTime.hour) { Int.MAX_VALUE }
        val minutes = rememberPagerState(initialPage = TimeWheelCenterPage + initialTime.minute) { Int.MAX_VALUE }
        val seconds = rememberPagerState(initialPage = TimeWheelCenterPage + initialTime.second) { Int.MAX_VALUE }
        val canConfirm = !hours.isScrollInProgress && !minutes.isScrollInProgress && !seconds.isScrollInProgress

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
                    TimePickerWheel(hours, 24, "时", columnWidth, rowHeight, style)
                    TimePickerWheel(minutes, 60, "分", columnWidth, rowHeight, style)
                    TimePickerWheel(seconds, 60, "秒", columnWidth, rowHeight, style)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PrimaryTextButton(
                    text = "确定",
                    style = style,
                    enabled = canConfirm,
                    padding = PaddingValues(horizontal = textHeight * 0.65f, vertical = textHeight * 0.2f),
                    onClick = {
                        onTimeChanged(LocalTime(
                            hour = (hours.settledPage - TimeWheelCenterPage).mod(24),
                            minute = (minutes.settledPage - TimeWheelCenterPage).mod(60),
                            second = (seconds.settledPage - TimeWheelCenterPage).mod(60)
                        ))
                    }
                )
            }
        }
    }
}