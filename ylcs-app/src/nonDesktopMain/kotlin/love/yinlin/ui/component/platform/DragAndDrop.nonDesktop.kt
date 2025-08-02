package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.dragAndDrop(
    enabled: Boolean,
    flag: Int,
    onDrop: (DropResult) -> Unit
): Modifier = this