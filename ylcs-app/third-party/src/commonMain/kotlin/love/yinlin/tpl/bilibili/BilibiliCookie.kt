package love.yinlin.tpl.bilibili

import androidx.compose.runtime.Stable
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import love.yinlin.foundation.http.NetCookie

@Stable
data class BilibiliCookie(
    val buvid3: String,
    val bnut: String,
) {
    val asCookies: NetCookie = NetCookie([
        Cookie("buvid3", buvid3, encoding = CookieEncoding.RAW),
        Cookie("bnut", bnut, encoding = CookieEncoding.RAW)
    ])
}