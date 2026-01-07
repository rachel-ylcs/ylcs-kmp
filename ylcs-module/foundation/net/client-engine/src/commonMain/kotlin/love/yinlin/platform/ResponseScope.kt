package love.yinlin.platform

import io.ktor.http.Cookie
import io.ktor.http.Headers

interface ResponseScope<Body> {
    val headers: Headers
    val cookies: List<Cookie>
    val body: Body
    val bodyString: String
    val bodyBytes: ByteArray
}