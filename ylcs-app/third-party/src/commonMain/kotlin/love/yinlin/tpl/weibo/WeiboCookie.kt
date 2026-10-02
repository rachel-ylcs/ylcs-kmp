package love.yinlin.tpl.weibo

import androidx.compose.runtime.Stable
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import love.yinlin.foundation.http.NetCookie

@Stable
data class WeiboCookie(
    val sub: String,
    val subp: String,
    val xsrfToken: String
) {
    val asCookies: NetCookie = NetCookie([
        Cookie("SUB", sub, encoding = CookieEncoding.RAW),
        Cookie("SUBP", subp, encoding = CookieEncoding.RAW),
        Cookie("XSRF-TOKEN", xsrfToken, encoding = CookieEncoding.RAW)
    ])
}