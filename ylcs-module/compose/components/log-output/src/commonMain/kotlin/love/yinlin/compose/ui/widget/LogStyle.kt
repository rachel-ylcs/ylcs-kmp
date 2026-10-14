package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable

@Stable
data class LogStyle(
    val foreground: AnsiColor? = null,
    val background: AnsiColor? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
)