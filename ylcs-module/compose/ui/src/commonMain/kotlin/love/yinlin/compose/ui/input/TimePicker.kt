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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.measureTextHeight

// 24 和 60 的公倍数，确保无限页映射到实际时、分、秒时不会偏移。
private const val TimeWheelCenterPage = 1_000_000_080

@Composable
private fun TimePickerWheel(
    state: PagerState,
    unit: String,
    range: Int,
    columnWidth: androidx.compose.ui.unit.Dp,
    rowHeight: androidx.compose.ui.unit.Dp,
    style: TextStyle
) {
    val scope = rememberCoroutineScope()
    val normalColor = LocalColor.current
    val fadedColor = LocalColorVariant.current
    val activeColor = Theme.color.primary

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
            0 -> activeColor
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
 * 中文时分秒滚轮选择面板，24 小时制。
 *
 * 三列滚轮独立滑动并自动吸附到中央选中行；点击旁边数字也可定位。
 * 仅点击“确定”时通过 [onTimeChanged] 提交当前时分秒；“取消”恢复 [time]，
 * 并调用 [onCancel]，由外部决定是否关闭承载此面板的弹窗。
 * 组件本身可直接显示在 Gallery 或由调用方放入弹窗，尺寸随 [style] 缩放。
 */
@Composable
fun TimePicker(
    time: LocalTime,
    onTimeChanged: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = Theme.typography.v8,
    onCancel: () -> Unit = {}
) {
    val textHeight = measureTextHeight("00", style)
    val rowHeight = textHeight * 2.2f
    val columnWidth = textHeight * 4.4f
    val outerPadding = textHeight * 0.8f
    val contentWidth = columnWidth * 3
    val scope = rememberCoroutineScope()

    key(time) {
        val hours = rememberPagerState(initialPage = TimeWheelCenterPage + time.hour) { Int.MAX_VALUE }
        val minutes = rememberPagerState(initialPage = TimeWheelCenterPage + time.minute) { Int.MAX_VALUE }
        val seconds = rememberPagerState(initialPage = TimeWheelCenterPage + time.second) { Int.MAX_VALUE }

        Surface(
            modifier = modifier,
            contentPadding = PaddingValues(outerPadding),
            shape = Theme.shape.v4,
            tonalLevel = 1
        ) {
            Column(
                modifier = Modifier.width(contentWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(textHeight * 0.65f)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    "时分秒".forEach { unit ->
                        Box(
                            modifier = Modifier.width(columnWidth).height(rowHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            SimpleClipText(unit.toString(), style = style, color = LocalColorVariant.current)
                        }
                    }
                }

                Box(modifier = Modifier.size(contentWidth, rowHeight * 5)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(rowHeight).align(Alignment.Center)
                            .clip(Theme.shape.v5)
                            .background(Theme.color.primaryContainer.copy(alpha = 0.3f))
                    )

                    Row(modifier = Modifier.fillMaxSize()) {
                        TimePickerWheel(hours, "时", 24, columnWidth, rowHeight, style)
                        TimePickerWheel(minutes, "分", 60, columnWidth, rowHeight, style)
                        TimePickerWheel(seconds, "秒", 60, columnWidth, rowHeight, style)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(text = "取消", style = style, onClick = {
                        scope.launch {
                            hours.animateScrollToPage(TimeWheelCenterPage + time.hour)
                            minutes.animateScrollToPage(TimeWheelCenterPage + time.minute)
                            seconds.animateScrollToPage(TimeWheelCenterPage + time.second)
                            onCancel()
                        }
                    })
                    PrimaryTextButton(text = "确定", style = style, onClick = {
                        onTimeChanged(LocalTime(
                            hour = (hours.settledPage - TimeWheelCenterPage).mod(24),
                            minute = (minutes.settledPage - TimeWheelCenterPage).mod(60),
                            second = (seconds.settledPage - TimeWheelCenterPage).mod(60)
                        ))
                    })
                }
            }
        }
    }
}