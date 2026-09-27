package love.yinlin.tpl.douyin

import androidx.compose.runtime.Stable
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding

@Stable
data class DouyinCookie(
    val ttwid: String,
    val fp: String,
    val msToken: String,
    val uifid: String,
) {
    val asCookies: List<Cookie> get() = [
        Cookie("ttwid", ttwid, encoding = CookieEncoding.RAW),
        Cookie("s_v_web_id", fp, encoding = CookieEncoding.RAW),
        Cookie("msToken", msToken, encoding = CookieEncoding.RAW),
        Cookie("UIFID_TEMP", uifid, encoding = CookieEncoding.RAW),
        Cookie("UIFID", uifid, encoding = CookieEncoding.RAW),
    ]
}