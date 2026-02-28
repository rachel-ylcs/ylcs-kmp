package love.yinlin.compose.ui.floating

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
data class FABAction(
    val iconProvider: () -> ImageVector,
    val tipProvider: () -> String = { "" },
    val onClick: (suspend () -> Unit)? = null,
    val backgroundColorProvider: (@Composable () -> Color)? = null,
    val contentColorProvider: (@Composable () -> Color)? = null,
    val enabledProvider: () -> Boolean = { true },
)