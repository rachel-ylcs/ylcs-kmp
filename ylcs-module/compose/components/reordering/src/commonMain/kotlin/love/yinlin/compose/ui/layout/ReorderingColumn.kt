package love.yinlin.compose.ui.layout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 少量条目的非滚动 Column，可嵌入已有滚动页面。拖动期间只更新内部预览，回落动画结束后
 * onSettle 按原始列表索引提交一次；若回落中开始下一次拖动，会先提交上一手势。
 * 调用方同步移动 list 中的元素即可。key 必须唯一且稳定。
 * 拖动范围限制在容器内部，绘制内容也按容器边界裁剪。
 * 默认激活样式由框架提供，可通过 [LocalReorderingItemStyle] 覆盖。
 */
@Composable
fun <T> ReorderingColumn(
    list: List<T>,
    key: (T) -> Any,
    onSettle: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ReorderingItemScope.(item: T, isDragging: Boolean) -> Unit,
) = ReorderingList(
    list = list,
    key = key,
    onSettle = onSettle,
    modifier = modifier,
    orientation = Orientation.Vertical,
    verticalArrangement = verticalArrangement,
    horizontalAlignment = horizontalAlignment,
    content = content,
)
