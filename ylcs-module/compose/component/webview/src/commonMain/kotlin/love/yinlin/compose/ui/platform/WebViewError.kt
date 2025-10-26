package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Stable

@Stable
data class WebViewError(
    val code: Long,
    val description: String
)