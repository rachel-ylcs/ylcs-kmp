package love.yinlin.foundation

import io.ktor.http.*

class RequestScope @PublishedApi internal constructor() {
    private var contentType: ContentType = ContentType.Text.Plain
    private val defaultHeaders: Headers get() = headers {
        append(HttpHeaders.ContentType, contentType.toString())
        append(HttpHeaders.Accept, ContentType.Any.toString())
    }

    var method: HttpMethod = HttpMethod.Get
    var url: String = ""
    var data: ByteArray? = null
    var form: Map<String, String>? = null
        set(value) {
            field = value
            contentType = if (value == null) ContentType.Text.Plain else ContentType.Application.FormUrlEncoded
        }
    var headers: Headers? = null
    var cookies: List<Cookie>? = null

    @PublishedApi
    internal fun buildHeaders(builder: HeadersBuilder) = builder.apply {
        appendAll(defaultHeaders)
        headers?.let(::appendAll)
        cookies?.let {
            append(HttpHeaders.Cookie, it.joinToString("; ", transform = ::renderCookieHeader))
        }
    }
}