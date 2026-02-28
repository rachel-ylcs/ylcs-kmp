package love.yinlin.compose.ui.node

import androidx.compose.ui.Modifier

actual fun Modifier.dragAndDrop(
    enabled: Boolean,
    flag: DragFlag,
    onDrop: (DropResult) -> Unit
): Modifier = this