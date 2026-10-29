package love.yinlin.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * 将内容区域设为当前桌面窗口的拖动区域，离开组合后自动注销。
 * 子组件优先处理指针事件；clickable 等消费事件后不会触发窗口拖动。
 * 可用于自定义标题栏、悬浮窗口或任意需要移动所属窗口的区域。
 */
@Composable
expect fun WindowScope.DragArea(
    enabled: Boolean = true,
    onLongClick: ((Offset) -> Unit)? = null,
    onDoubleClick: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)
