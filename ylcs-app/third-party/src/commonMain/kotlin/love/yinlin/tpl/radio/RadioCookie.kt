package love.yinlin.tpl.radio

import androidx.compose.runtime.Stable
import io.ktor.http.Cookie
import love.yinlin.foundation.http.NetCookie

@Stable
data class RadioCookie(val nmtid: String) {
    val asCookies: NetCookie = NetCookie([
        Cookie("NMTID", nmtid),
    ])
}