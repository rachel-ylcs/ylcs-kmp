package love.yinlin.io

import kotlinx.io.RawSource
import love.yinlin.extension.catchingDefault

class Sources<S : RawSource>(private val sources: MutableList<S> = mutableListOf()) : AutoCloseable, MutableList<S> by sources {
    override fun close() {
        for (source in sources) source.close()
    }
}

inline fun <T, S : RawSource> Collection<T>.safeToSources(crossinline block: (T) -> S?): Sources<S>? {
    val sources = Sources<S>()
    return catchingDefault({
        sources.close()
        null
    }) {
        for (item in this) block(item)?.let { sources += it }
        sources
    }
}