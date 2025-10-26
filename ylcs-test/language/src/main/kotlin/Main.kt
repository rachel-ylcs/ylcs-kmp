import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.thread
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun runTaskWithSuspend(): String = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        continuation.resumeWithException(CancellationException())
    }
    thread {
        Thread.sleep(5000)
        error("1")
        continuation.resume("result")
    }
}

suspend fun main() {
    println("start")
    val result = runTaskWithSuspend()
    println(result)
    delay(10000)
}