package love.yinlin.ui.component.platform

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    topBar: (@Composable RowScope.() -> Unit)?
) {

}