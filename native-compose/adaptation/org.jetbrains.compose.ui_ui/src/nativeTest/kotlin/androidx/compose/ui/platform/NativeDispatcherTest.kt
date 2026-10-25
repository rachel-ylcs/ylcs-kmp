package androidx.compose.ui.platform

import kotlinx.coroutines.*
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.*

class NativeDispatcherTest {
    @Test
    fun dispatchIsQueuedAndReentrantWorkKeepsFifoOrder() {
        val dispatcher = NativeUiDispatcher()
        val order = mutableListOf<Int>()
        try {
            dispatcher.dispatch(EmptyCoroutineContext) {
                order.add(1)
                dispatcher.dispatch(EmptyCoroutineContext) { order.add(3) }
            }
            dispatcher.dispatch(EmptyCoroutineContext) { order.add(2) }
            assertTrue(order.isEmpty())
            dispatcher.runCurrent()
            val expected: List<Int> = [1, 2, 3]
            assertEquals(expected, order)
        }
        finally { dispatcher.close() }
    }

    @Test
    fun delayedResumptionRunsOnlyWhenTheHostDrainsItsQueue() = runBlocking {
        val dispatcher = NativeUiDispatcher()
        val queued = CompletableDeferred<Unit>()
        dispatcher.wake = { queued.complete(Unit) }
        var resumed = false
        try {
            val work = launch(dispatcher, start = CoroutineStart.UNDISPATCHED) {
                delay(10)
                resumed = true
            }
            withTimeout(10_000) { queued.await() }
            assertFalse(resumed)
            dispatcher.runCurrent()
            assertTrue(resumed)
            work.join()
        }
        finally { dispatcher.close() }
    }

    @Test
    fun closeCancelsSuspendedWorkAndRunsItsFinallyBlock() {
        val dispatcher = NativeUiDispatcher()
        var disposed = false
        val work = CoroutineScope(dispatcher.context).launch {
            try { awaitCancellation() }
            finally { disposed = true }
        }
        dispatcher.runCurrent()
        dispatcher.close()
        dispatcher.close()
        assertTrue(work.isCancelled && disposed)
    }

    @Test
    fun aFailingQueuedCallbackDoesNotDiscardCancellationCleanup() {
        val dispatcher = NativeUiDispatcher()
        var disposed = false
        CoroutineScope(dispatcher.context).launch {
            try { awaitCancellation() }
            finally { disposed = true }
        }
        dispatcher.runCurrent()
        dispatcher.dispatch(EmptyCoroutineContext) { error("queued failure") }
        val failure = assertFailsWith<IllegalStateException> { dispatcher.close() }
        assertEquals("queued failure", failure.message)
        assertTrue(disposed)
        dispatcher.close()
    }

    @Test
    fun oneHostClaimsTheThreadAndClosingAllowsAReplacement() {
        val first = nativeUiDispatcher().claim()
        try { assertFailsWith<IllegalStateException> { nativeUiDispatcher().claim() } }
        finally { first.close() }
        val second = nativeUiDispatcher().claim()
        assertNotSame(first, second)
        second.close()
    }
}
