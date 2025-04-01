package love.yinlin.platform

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

expect object Coroutines {
	suspend fun <T> main(block: suspend CoroutineScope.() -> T): T
	suspend fun <T> cpu(block: suspend CoroutineScope.() -> T): T
	suspend fun <T> io(block: suspend CoroutineScope.() -> T): T
	suspend fun <T> wait(block: suspend CoroutineScope.() -> T): T
	suspend fun <T> timeout(limit: Long, block: suspend CoroutineScope.() -> T): T
}

inline fun <T> Continuation<T?>.safeResume(crossinline block: () -> Unit) {
	try { block() }
	catch (_: Throwable) { this.resume(null) }
}