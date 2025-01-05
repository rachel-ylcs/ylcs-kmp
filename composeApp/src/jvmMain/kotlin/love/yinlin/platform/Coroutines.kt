package love.yinlin.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object Coroutines {
	actual suspend fun <T> main(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Main, block)
	actual suspend fun <T> cpu(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Default, block)
	actual suspend fun <T> io( block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)
}