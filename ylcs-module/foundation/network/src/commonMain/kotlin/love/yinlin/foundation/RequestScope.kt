package love.yinlin.foundation

import io.ktor.http.*
import love.yinlin.extension.then
import love.yinlin.foundation.http.NetHeader

class RequestScope @PublishedApi internal constructor() {
    var contentType: ContentType = ContentType.Text.Plain

    var method: HttpMethod = HttpMethod.Get
    var url: String = ""
    var data: ByteArray? = null
    var form: Map<String, String>? = null
        set(value) {
            field = value
            contentType = if (value == null) ContentType.Text.Plain else ContentType.Application.FormUrlEncoded
        }
    var headers: NetHeader? = null
    var cookies: List<Cookie>? = null

    @PublishedApi
    internal fun buildHeaders(builder: HeadersBuilder) = builder.apply {
        append(HttpHeaders.ContentType, contentType.toString())
        append(HttpHeaders.Accept, ContentType.Any.toString())
        append(HttpHeaders.Connection, NetHeader.KeepAlive)
        headers?.then(::appendAll)
        cookies?.then {
            append(HttpHeaders.Cookie, it.joinToString("; ", transform = ::renderCookieHeader))
        }
    }
}