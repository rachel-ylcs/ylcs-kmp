package love.yinlin.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalContracts::class)
object Coroutines {
    suspend inline fun <T> with(context: CoroutineContext, noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withContext(context, block)
    }

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

    suspend inline fun <T> timeout(limit: Int, noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withTimeout(limit.toLong(), block)
    }

    suspend fun isActive(): Boolean = currentCoroutineContext().isActive
    suspend fun requireActive() = currentCoroutineContext().ensureActive()

    suspend inline fun <T> sync(crossinline block: (SyncFuture<T>) -> Unit): T? = suspendCancellableCoroutine { continuation ->
        block(SyncFuture(continuation))
    }

    suspend inline fun catching(block: suspend () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        try { block() }
        catch (e: CancellationException) { throw e }
        catch (_: Throwable) { }
    }

    suspend inline fun catchingError(block: suspend () -> Unit): Throwable? {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return try {
            block()
            null
        }
        catch (e: CancellationException) { throw e }
        catch (e: Throwable) { e }
    }

    suspend inline fun <R> catchingNull(block: suspend () -> R): R? {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return try { block() }
        catch (e: CancellationException) { throw e }
        catch (_: Throwable) { null }
    }

    suspend inline fun <R> catchingDefault(default: R, block: suspend () -> R): R {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return try { block() }
        catch (e: CancellationException) { throw e }
        catch (_: Throwable) { default }
    }

    suspend inline fun <R> catchingDefault(default: (Throwable) -> R, block: suspend () -> R): R {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: Throwable) { default(e) }
    }
}