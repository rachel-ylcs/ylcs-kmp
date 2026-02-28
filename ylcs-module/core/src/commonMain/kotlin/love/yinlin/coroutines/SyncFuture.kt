package love.yinlin.coroutines

import kotlinx.coroutines.CancellableContinuation
import love.yinlin.extension.catchingError

class SyncFuture<T>(private val continuation: CancellableContinuation<T?>) {
    fun send() {
        if (!continuation.isCompleted) continuation.resumeWith(Result.success(null))
    }

    fun send(result: T?) {
        if (!continuation.isCompleted) continuation.resumeWith(Result.success(result))
    }

    inline fun send(block: () -> T) {
        catchingError {
            send(block())
        }?.let { send() }
    }

    fun cancel() { continuation.cancel() }

    inline fun catching(block: () -> Unit) {
        catchingError(block = block)?.let { send() }
    }

    fun clean(block: (Throwable?) -> Unit) = continuation.invokeOnCancellation(block)
}