package love.yinlin.picker

import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catching

internal suspend fun <T : AutoCloseable> pickerResource(block: suspend () -> T?): T? {
    var resource: T? = null
    var transferred = false
    try {
        val result = Coroutines.io { block().also { resource = it } }
        transferred = true
        return result
    }
    finally {
        if (!transferred) catching { resource?.close() }
    }
}
