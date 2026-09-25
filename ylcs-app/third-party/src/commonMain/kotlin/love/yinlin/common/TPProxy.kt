package love.yinlin.common

import androidx.compose.runtime.Stable
import io.ktor.http.Headers
import io.ktor.http.headers
import io.ktor.util.appendAll
import love.yinlin.platform.Platform

@Stable
object TPProxy {
    // 暂时未实现Web在服务器端的转发
    internal fun proxy(url: String): String = Platform.use(*Platform.Web, ifTrue = { url }, ifFalse = { url })
    internal fun proxyRes(url: String): String = Platform.use(*Platform.Web, ifTrue = { url }, ifFalse = { url })
    internal fun proxyHeader(rawHeaders: Map<String, String>): Headers = headers {
        Platform.use(*Platform.Web,
            ifTrue = { appendAll(rawHeaders) },
            ifFalse = { appendAll(rawHeaders) }
        )
    }
}