package love.yinlin.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

object Coroutines {
    suspend inline fun <T> main(noinline block: suspend CoroutineScope.() -> T): T = withContext(mainContext, block)
    suspend inline fun <T> cpu(noinline block: suspend CoroutineScope.() -> T): T = withContext(cpuContext, block)
    suspend inline fun <T> io(noinline block: suspend CoroutineScope.() -> T): T = withContext(ioContext, block)
    suspend inline fun <T> wait(noinline block: suspend CoroutineScope.() -> T): T = withContext(waitContext, block)
    suspend inline fun <T> timeout(limit: Int, noinline block: suspend CoroutineScope.() -> T): T = withTimeout(limit.toLong(), block)

    fun startMain(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(mainContext).launch(block = block)
    fun startCPU(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(cpuContext).launch(block = block)
    fun startIO(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(ioContext).launch(block = block)
    fun startWait(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(waitContext).launch(block = block)
}

inline fun <T> Continuation<T?>.safeResume(crossinline block: () -> Unit) {
    try { block() }
    catch (_: Throwable) { this.resume(null) }
}

expect val mainContext: CoroutineContext
expect val cpuContext: CoroutineContext
expect val ioContext: CoroutineContext
expect val waitContext: CoroutineContext