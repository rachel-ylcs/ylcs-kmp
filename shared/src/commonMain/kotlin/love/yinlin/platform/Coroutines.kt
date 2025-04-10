package love.yinlin.platform

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

expect object Coroutines {
    suspend inline fun <T> main(noinline block: suspend CoroutineScope.() -> T): T
    suspend inline fun <T> cpu(noinline block: suspend CoroutineScope.() -> T): T
    suspend inline fun <T> io(noinline block: suspend CoroutineScope.() -> T): T
    suspend inline fun <T> wait(noinline block: suspend CoroutineScope.() -> T): T
    suspend inline fun <T> timeout(limit: Int, noinline block: suspend CoroutineScope.() -> T): T
}

inline fun <T> Continuation<T?>.safeResume(crossinline block: () -> Unit) {
    try { block() }
    catch (_: Throwable) { this.resume(null) }
}