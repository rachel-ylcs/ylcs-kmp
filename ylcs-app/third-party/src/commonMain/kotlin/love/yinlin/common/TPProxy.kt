package love.yinlin.common

import androidx.compose.runtime.Stable
import love.yinlin.platform.Platform

@Stable
object TPProxy {
    // 暂时未实现Web在服务器端的转发
    internal fun proxy(url: String): String = Platform.use(*Platform.Web, ifTrue = { url }, ifFalse = { url })
    internal fun proxyRes(url: String): String = Platform.use(*Platform.Web, ifTrue = { url }, ifFalse = { url })
}