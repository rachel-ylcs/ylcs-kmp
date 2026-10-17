package love.yinlin.compose.ui.layout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 少量条目的非滚动 Row，拖柄沿水平方向移动，支持 RTL 和不同条目宽度。
 * 与 [ReorderingColumn] 相同，拖动期间只更新内部预览，回落后以原始列表索引提交一次 onSettle。
 * 回落中可以继续拖动，下一手势开始前会先提交上一手势，不丢失已经完成的移动。
 * 可通过 modifier 接入 horizontalScroll，普通 Row 自身不执行边缘自动滚动。
 * 拖动范围限制在 Row 的内容边界内部，绘制内容也按边界裁剪。
 * 默认激活样式由框架提供，可通过 [LocalReorderingItemStyle] 覆盖。
 */
@Composable
fun <T> ReorderingRow(
    list: List<T>,
    key: (T) -> Any,
    onSettle: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable ReorderingItemScope.(item: T, isDragging: Boolean) -> Unit,
) = ReorderingList(
    list = list,
    key = key,
    onSettle = onSettle,
    modifier = modifier,
    orientation = Orientation.Horizontal,
    horizontalArrangement = horizontalArrangement,
    verticalAlignment = verticalAlignment,
    content = content,
)
