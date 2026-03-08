package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

@Stable
fun Modifier.pointerIcon(icon: PointerIcon, enabled: Boolean = true): Modifier = if (enabled) this.pointerHoverIcon(icon) else this