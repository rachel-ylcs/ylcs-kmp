package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
data class WebViewError(
    val code: Long,
    val description: String
)