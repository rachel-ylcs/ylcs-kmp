package love.yinlin.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalContracts::class)
object Coroutines {
    suspend inline fun <T> with(context: CoroutineContext, noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withContext(context, block)
    }

    suspend inline fun <T> with(context1: CoroutineContext, context2: CoroutineContext, noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withContext(context1 + context2, block)
    }

    suspend inline fun <T> main(noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withContext(mainContext, block)
    }

    suspend inline fun <T> mainImmediate(noinline block: suspend CoroutineScope.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return withContext(mainImmediateContext, block)
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
        return withTimeout(limit.milliseconds, block)
    }

    inline fun dispatchUI(crossinline action: () -> Unit) {
        val dispatcher = Dispatchers.Main.immediate
        val defaultContext = emptyContext
        if (dispatcher.isDispatchNeeded(defaultContext)) {
            dispatcher.dispatch(defaultContext) { action() }
        }
        else action()
    }

    suspend inline fun <T> sync(crossinline block: (SyncFuture<T>) -> Unit): T? = suspendCancellableCoroutine { continuation ->
        block(SyncFuture(continuation))
    }
}