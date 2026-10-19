package love.yinlin.io

import kotlinx.io.RawSource
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.extension.cleaning
import love.yinlin.extension.then

class Sources<S : RawSource>(private val sources: MutableList<S> = []) : AutoCloseable, MutableList<S> by sources {
    override fun close() {
        var failure: Throwable? = null
        for (source in sources) {
            // 包括取消异常在内，完成全部资源的清理后再传播第一个异常。
            catchingError {
                source.close()
            }?.then { error ->
                val first = failure
                if (first == null) failure = error
                else if (first !== error) first.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }
}

inline fun <T, S : RawSource> Collection<T>.safeToSources(block: (T) -> S?): Sources<S>? {
    val sources = Sources<S>()
    var completed = false
    var failure: Throwable? = null
    cleaning({
        val result = catchingNull {
            try {
                for (item in this) block(item)?.then { sources += it }
                sources
            }
            catch (error: Throwable) {
                failure = error
                throw error
            }
        }
        completed = result != null
        return result
    }) {
        if (!completed) {
            try {
                sources.close()
            }
            catch (cleanup: Throwable) {
                val primary = failure
                if (primary == null) throw cleanup
                else if (primary !== cleanup) primary.addSuppressed(cleanup)
            }
        }
    }
}