package love.yinlin.platform

import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpMethod

interface RequestScope {
    var method: HttpMethod
    var url: String
    var data: ByteArray

    fun headers(block: HeadersBuilder.() -> Unit)
}