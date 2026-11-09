package love.yinlin.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * 将内容区域设为当前桌面窗口的拖动区域，离开组合后自动注销
 */
@Composable
expect fun WindowScope.DragArea(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: ((Offset) -> Unit)? = null,
    onDoubleClick: ((Offset) -> Unit)? = null,
    content: @Composable () -> Unit,
)
