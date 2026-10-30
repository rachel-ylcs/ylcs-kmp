package love.yinlin.foundation.picker

import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catching
import love.yinlin.extension.cleaning

internal suspend fun <T : AutoCloseable> pickerResource(block: suspend () -> T?): T? {
    var resource: T? = null
    var transferred = false

    cleaning({
        val result = Coroutines.io {
            block().also { resource = it }
        }
        transferred = true
        return result
    }) {
        if (!transferred) catching { resource?.close() }
    }
}
