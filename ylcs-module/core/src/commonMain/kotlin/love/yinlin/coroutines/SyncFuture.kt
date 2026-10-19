package love.yinlin.coroutines

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.InternalCoroutinesApi
import love.yinlin.extension.catchingError

@OptIn(InternalCoroutinesApi::class)
class SyncFuture<T>(private val continuation: CancellableContinuation<T?>) {
    fun send(result: T?) {
        val token = continuation.tryResume(result) ?: return
        continuation.completeResume(token)
    }

    fun send() {
        val token = continuation.tryResume(null) ?: return
        continuation.completeResume(token)
    }

    inline fun send(block: () -> T) {
        val _ = catchingError { send(block()) } ?: return
        send()
    }

    fun cancel() { continuation.cancel() }

    inline fun catching(block: () -> Unit) {
        val _ = catchingError(block = block) ?: return
        send()
    }

    fun clean(block: (Throwable?) -> Unit) = continuation.invokeOnCancellation(block)
}