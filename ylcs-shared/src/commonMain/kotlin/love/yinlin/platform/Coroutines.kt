package love.yinlin.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

@OptIn(ExperimentalContracts::class)
object Coroutines {
    suspend inline fun <T> main(noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withContext(mainContext, block)
    }

    suspend inline fun <T> cpu(noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withContext(cpuContext, block)
    }

    suspend inline fun <T> io(noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withContext(ioContext, block)
    }

    suspend inline fun <T> wait(noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withContext(waitContext, block)
    }

    suspend inline fun <T> timeout(limit: Int, noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withTimeout(limit.toLong(), block)
    }

    suspend fun isActive(): Boolean = currentCoroutineContext().isActive
    suspend fun startCurrent(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(currentCoroutineContext()).launch(block = block)
    fun startMain(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(mainContext).launch(block = block)
    fun startCPU(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(cpuContext).launch(block = block)
    fun startIO(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(ioContext).launch(block = block)
    fun startWait(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(waitContext).launch(block = block)
}

inline fun <T> Continuation<T?>.safeResume(crossinline block: () -> Unit) {
    try {
        block()
    }
    catch (_: Throwable) {
        this.resume(null)
    }
}

expect val mainContext: CoroutineContext
expect val cpuContext: CoroutineContext
expect val ioContext: CoroutineContext
expect val waitContext: CoroutineContext