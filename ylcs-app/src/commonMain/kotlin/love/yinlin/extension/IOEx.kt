package love.yinlin.extension

import kotlinx.io.RawSource

class Sources<S : RawSource>(
	private val sources: MutableList<S> = mutableListOf()
) : AutoCloseable, MutableList<S> by sources {
	override fun close() {
		for (source in sources) source.close()
	}
}

inline fun <T, S : RawSource> Collection<T>.safeToSources(crossinline block: (T) -> S?): Sources<S>? {
	val sources = Sources<S>()
	return try {
		for (item in this) block(item)?.let { sources += it }
		sources
	}
	catch (_: Throwable) {
		sources.close()
		null
	}
}