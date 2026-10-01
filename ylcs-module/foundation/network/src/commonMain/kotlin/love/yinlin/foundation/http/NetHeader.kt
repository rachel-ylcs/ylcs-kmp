package love.yinlin.foundation.http

import io.ktor.http.Headers
import io.ktor.http.HeadersImpl

class NetHeader(rawItems: Map<String, List<String>>) : Headers by HeadersImpl(rawItems) {
    constructor(vararg items: Pair<String, String>) : this(items.associate { [k, v] -> k to [v] })

    @Suppress("ConstPropertyName")
    companion object {
        operator fun of(vararg items: Pair<String, String>): NetHeader = NetHeader(*items)

        val Empty = NetHeader()

        const val UserAgentDesktop = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36 Edg/155.0.0.0"
        const val UserAgentMobile = "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36 Edg/155.0.0.0 Mobile"
        const val AcceptLanguage = "zh-CN,zh;q=0.9,en;q=0.8"
        const val AcceptEncoding = "gzip,deflate,br"
        const val KeepAlive = "keep-alive"
    }
}