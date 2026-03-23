package love.yinlin.compose.cache

import kotlin.jvm.JvmInline

@JvmInline
value class UrlCacheSource(val url: String) : CacheSource {
    override val key: String get() = XXHash64.hash(url)
}