@file:OptIn(InternalCoroutinesApi::class)

package androidx.compose.ui.platform

import androidx.compose.ui.internal.getCurrentThreadId
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.concurrent.Volatile
import kotlin.native.concurrent.ThreadLocal
import kotlin.time.Duration.Companion.milliseconds

/** A host-thread queue; background timers only enqueue resumptions and never execute UI work. */
internal class NativeUiDispatcher : CoroutineDispatcher(), Delay, AutoCloseable {
    private val ownerThread = getCurrentThreadId()
    private val lock = SynchronizedObject()
    private val queue = ArrayDeque<Runnable>()
    private val job = SupervisorJob()
    private val timers = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var closed = false
    private var closing = false
    private var claimed = false
    @Volatile
    var wake: () -> Unit = {}

    val context: CoroutineContext get() = this + job

    fun claim(): NativeUiDispatcher {
        check(!closed && !closing && !claimed) { "A Native Compose window already owns this host thread" }
        claimed = true
        return this
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val accepted = synchronized(lock) {
            if (closed) false
            else {
                queue.addLast(block)
                true
            }
        }
        if (accepted) wake()
        else {
            context[Job]?.cancel(CancellationException("Native UI host is closed"))
            Dispatchers.Default.dispatch(context, block)
        }
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val timer = timers.launch {
            delay(timeMillis.milliseconds)
            continuation.resume(Unit)
        }
        continuation.invokeOnCancellation { timer.cancel() }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val timer = timers.launch {
            delay(timeMillis.milliseconds)
            dispatch(context, block)
        }
        return DisposableHandle { timer.cancel() }
    }

    fun runCurrent() {
        check(getCurrentThreadId() == ownerThread) { "Native UI work must run on its host thread" }
        while (true) {
            val next = synchronized(lock) { queue.removeFirstOrNull() } ?: break
            next.run()
        }
    }

    override fun close() {
        if (closed || closing) return
        check(getCurrentThreadId() == ownerThread) { "Close the Native UI dispatcher on its host thread" }
        closing = true
        job.cancel()
        timers.cancel()
        var failure: Throwable? = null
        try {
            while (true) {
                val next = synchronized(lock) {
                    val next = queue.removeFirstOrNull()
                    if (next == null) closed = true
                    next
                } ?: break
                try { next.run() }
                catch (error: Throwable) {
                    val previous = failure
                    if (previous == null) failure = error
                    else if (previous !== error) previous.addSuppressed(error)
                }
            }
        }
        finally {
            synchronized(lock) { closed = true }
            wake = {}
            if (currentNativeUiDispatcher === this) currentNativeUiDispatcher = null
        }
        failure?.let { throw it }
    }
}

@ThreadLocal
private var currentNativeUiDispatcher: NativeUiDispatcher? = null

internal fun nativeUiDispatcher(): NativeUiDispatcher = currentNativeUiDispatcher ?: NativeUiDispatcher().also { currentNativeUiDispatcher = it }
