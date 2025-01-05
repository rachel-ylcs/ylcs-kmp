package love.yinlin.platform

import kotlinx.coroutines.CoroutineScope

expect object Coroutines {
	suspend fun <T> main(block: suspend CoroutineScope.() -> T): T
	suspend fun <T> cpu(block: suspend CoroutineScope.() -> T): T
	suspend fun <T> io(block: suspend CoroutineScope.() -> T): T
}