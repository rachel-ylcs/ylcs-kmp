package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * 接收拖入此区域的文件或文本，通过 [DragFlag.or] 组合允许的数据类型。
 * 同一份数据同时包含文件和文本且两者都允许时，优先返回文件；文件不会被自动读取。
 * [enabled] 为 false 时不接收拖放；Android 和 iOS 保持原 Modifier。
 */
@Stable
expect fun Modifier.dragDrop(
    enabled: Boolean,
    flag: DragFlag,
    onDrop: (DropResult) -> Unit
): Modifier
