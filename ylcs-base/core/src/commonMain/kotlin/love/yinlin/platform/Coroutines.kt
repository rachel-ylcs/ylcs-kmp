package love.yinlin.platform

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import love.yinlin.extension.catchingError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class SyncFuture<T>(private val continuation: CancellableContinuation<T?>) {
    fun send() = continuation.resumeWith(Result.success(null))
    fun send(result: T?) = continuation.resumeWith(Result.success(result))
    inline fun send(block: () -> T) { catchingError { send(block()) }?.let { send() } }
    fun cancel() { continuation.cancel(CancellationException("SyncFuture cancelled")) }
    inline fun catching(block: () -> Unit) { catchingError(block = block)?.let { send() } }
}

@OptIn(ExperimentalContracts::class)
data object Coroutines {
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
    suspend fun startCurrent(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit): Job =
        CoroutineScope(currentCoroutineContext()).launch(context = context, block = block)
    fun startMain(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(mainContext).launch(block = block)
    fun startCPU(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(cpuContext).launch(block = block)
    fun startIO(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(ioContext).launch(block = block)
    fun startWait(block: suspend CoroutineScope.() -> Unit): Job = CoroutineScope(waitContext).launch(block = block)

    suspend inline fun <T> sync(
        crossinline onCancel: () -> Unit = {},
        crossinline block: (SyncFuture<T>) -> Unit
    ): T? = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { onCancel() }
        block(SyncFuture(continuation))
    }
}

expect val mainContext: CoroutineContext
expect val cpuContext: CoroutineContext
expect val ioContext: CoroutineContext
expect val waitContext: CoroutineContext