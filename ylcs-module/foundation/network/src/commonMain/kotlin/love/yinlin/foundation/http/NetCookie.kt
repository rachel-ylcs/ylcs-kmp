package love.yinlin.foundation.http

import io.ktor.http.Cookie
import io.ktor.http.renderCookieHeader
import io.ktor.util.CaseInsensitiveMap
import kotlin.collections.putAll

class NetCookie private constructor(private val raw: RawMap) : Map<String, Cookie> by raw {
    private typealias RawMap = CaseInsensitiveMap<Cookie>

    @Suppress("unused")
    constructor(items: Map<String, Cookie>, dummy: Unit = Unit) : this(RawMap().also {
        it.putAll(items)
    })

    constructor(items: Map<String, String>) : this(RawMap().also {
        for ([k, v] in items) it[k] = Cookie(name = k, value = v)
    })

    @Suppress("unused")
    constructor(vararg items: Pair<String, Cookie>, dummy: Unit = Unit) : this(RawMap().also {
        it.putAll(items)
    })

    constructor(vararg items: Pair<String, String>) : this(RawMap().also {
        for ([k, v] in items) it[k] = Cookie(name = k, value = v)
    })

    constructor(items: List<Cookie>) : this(RawMap().also {
        for (cookie in items) {
            if (!cookie.value.equals("deleted", ignoreCase = true)) it[cookie.name] = cookie
        }
    })

    companion object {
        operator fun of(): NetCookie = Empty
        operator fun of(item: Pair<String, String>): NetCookie = NetCookie(item)
        operator fun of(vararg items: Pair<String, String>): NetCookie = NetCookie(*items)

        val Empty = NetCookie()
    }

    val asPlainText: String get() = raw.values.joinToString("; ", transform = ::renderCookieHeader)
}