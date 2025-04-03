package love.yinlin.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

actual object Coroutines {
    actual suspend inline fun <T> main(noinline block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Main, block)
    actual suspend inline fun <T> cpu(noinline block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Default, block)
    actual suspend inline fun <T> io(noinline block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)
    actual suspend inline fun <T> wait(noinline block: suspend CoroutineScope.() -> T): T = withContext(NonCancellable, block)
    actual suspend inline fun <T> timeout(limit: Int, noinline block: suspend CoroutineScope.() -> T): T = withTimeout(limit.toLong(), block)
}