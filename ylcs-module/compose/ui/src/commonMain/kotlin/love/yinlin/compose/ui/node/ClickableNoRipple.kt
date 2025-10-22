package love.yinlin.compose.ui.node

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.clickableNoRipple(enabled: Boolean = true, onClick: () -> Unit): Modifier = this.clickable(
	interactionSource = null,
	indication = null,
	enabled = enabled,
	onClick = onClick
)