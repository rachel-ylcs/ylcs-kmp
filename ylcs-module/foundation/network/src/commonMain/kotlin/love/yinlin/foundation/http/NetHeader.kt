package love.yinlin.foundation.http

import io.ktor.http.Headers
import io.ktor.util.CaseInsensitiveMap

class NetHeader private constructor(private val raw: RawMap) : Headers {
    private typealias RawMap = CaseInsensitiveMap<List<String>>

    @Suppress("unused")
    constructor(items: Map<String, List<String>>, dummy: Unit = Unit) : this(RawMap().also {
        it.putAll(items)
    })

    constructor(items: Map<String, String>) : this(RawMap().also {
        for ([k, v] in items) it[k] = [v]
    })

    @Suppress("unused")
    constructor(vararg items: Pair<String, List<String>>, dummy: Unit = Unit) : this(RawMap().also {
        it.putAll(items)
    })

    constructor(vararg items: Pair<String, String>) : this(RawMap().also {
        for ([k, v] in items) it[k] = [v]
    })

    @Suppress("ConstPropertyName")
    companion object {
        operator fun of(): NetHeader = Empty
        operator fun of(item: Pair<String, String>): NetHeader = NetHeader(item)
        operator fun of(vararg items: Pair<String, String>): NetHeader = NetHeader(*items)

        val Empty = NetHeader()

        const val UserAgentDesktop = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36 Edg/155.0.0.0"
        const val UserAgentMobile = "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36 Edg/155.0.0.0 Mobile"
        const val AcceptAll = "*/*"
        const val AcceptLanguageDefault = "zh-CN,zh;q=0.9,en;q=0.8"
        const val AcceptEncodingDefault = "gzip,deflate,br"
        const val ConnectionKeepAlive = "keep-alive"
    }

    override val caseInsensitiveName: Boolean = true

    override fun getAll(name: String): List<String>? = raw[name]

    override fun names(): Set<String> = raw.keys

    override fun entries(): Set<Map.Entry<String, List<String>>> = raw.entries

    override fun isEmpty(): Boolean = raw.isEmpty()
}