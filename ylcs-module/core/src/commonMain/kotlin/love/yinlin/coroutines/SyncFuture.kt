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