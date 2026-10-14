package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable

@Stable
data class LogSpan(
    val text: String,
    val style: LogStyle = LogStyle()
)