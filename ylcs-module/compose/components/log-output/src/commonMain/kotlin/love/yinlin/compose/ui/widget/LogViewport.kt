package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable

@Stable
internal data class LogViewport(
    val isScrollInProgress: Boolean,
    val canScrollForward: Boolean,
    val programmatic: Boolean,
    val userMoved: Boolean
)