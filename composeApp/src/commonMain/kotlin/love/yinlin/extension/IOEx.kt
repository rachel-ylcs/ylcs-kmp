package love.yinlin.extension

import kotlinx.io.RawSource

class Sources<S : RawSource> : AutoCloseable, Iterable<S> {
	private val sources = mutableListOf<S>()

	val size: Int get() = sources.size
	val isEmpty: Boolean get() = sources.isEmpty()
	val isNotEmpty: Boolean get() = sources.isNotEmpty()
	operator fun plusAssign(s: S) { sources += s }
	override fun iterator(): Iterator<S> = sources.iterator()

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