package love.yinlin.foundation

import io.ktor.http.Cookie
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode

interface ResponseScope<Body> {
    val status: HttpStatusCode
    val url: String
    val headers: Headers
    val cookies: List<Cookie>
    val rawBody: ByteArray
    val bodyString: String
    val body: Body
}