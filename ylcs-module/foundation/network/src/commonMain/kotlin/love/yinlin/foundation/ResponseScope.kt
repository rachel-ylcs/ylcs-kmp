package love.yinlin.foundation

import io.ktor.http.HttpStatusCode
import love.yinlin.foundation.http.NetCookie
import love.yinlin.foundation.http.NetHeader

interface ResponseScope<Body> {
    val status: HttpStatusCode
    val url: String
    val headers: NetHeader
    val cookies: NetCookie
    val rawBody: ByteArray
    val bodyString: String
    val body: Body
}