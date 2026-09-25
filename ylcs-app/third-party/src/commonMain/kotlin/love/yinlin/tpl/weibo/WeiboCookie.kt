package love.yinlin.tpl.weibo

import androidx.compose.runtime.Stable

@Stable
data class WeiboCookie(
    val sub: String,
    val subp: String,
    val xsrfToken: String
)