package love.yinlin.concurrent

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.sync.Mutex as CoroutinesMutex

class Mutex(locked: Boolean = false) {
    private val delegate = CoroutinesMutex(locked)
    val isLocked: Boolean get() = delegate.isLocked
    fun tryLock(): Boolean = delegate.tryLock()
    suspend fun lock() = delegate.lock()
    fun unlock() = delegate.unlock()

    @OptIn(ExperimentalContracts::class)
    suspend inline fun with(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        lock()
        try { block() } finally { unlock() }
    }
}