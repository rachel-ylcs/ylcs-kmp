package love.yinlin.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

actual object Coroutines {
	actual suspend fun <T> main(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Main, block)
	actual suspend fun <T> cpu(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Default, block)
	actual suspend fun <T> io( block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Default, block)
	actual suspend fun <T> wait(block: suspend CoroutineScope.() -> T): T = withContext(NonCancellable, block)
	actual suspend fun <T> timeout(limit: Long, block: suspend CoroutineScope.() -> T): T = withTimeout(limit, block)
}