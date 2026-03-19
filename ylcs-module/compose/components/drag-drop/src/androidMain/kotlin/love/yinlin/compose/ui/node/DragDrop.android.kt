package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
actual fun Modifier.dragDrop(
    enabled: Boolean,
    flag: DragFlag,
    onDrop: (DropResult) -> Unit
): Modifier = this