package love.yinlin.foundation

import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import love.yinlin.foundation.http.NetHeader

interface ResponseScope<Body> {
    val status: HttpStatusCode
    val url: String
    val headers: NetHeader
    val cookies: List<Cookie>
    val rawBody: ByteArray
    val bodyString: String
    val body: Body
}