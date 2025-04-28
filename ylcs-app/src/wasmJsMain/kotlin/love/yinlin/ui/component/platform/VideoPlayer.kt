package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.platform.UnsupportedComponent

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onBack: () -> Unit
) {
    UnsupportedComponent(modifier = modifier)
}