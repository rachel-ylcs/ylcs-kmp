package love.yinlin.foundation

import io.ktor.http.Cookie
import io.ktor.http.Headers

interface ResponseScope<Body> {
    val headers: Headers
    val cookies: List<Cookie>
    val rawBody: ByteArray
    val bodyString: String
    val body: Body
}