package androidx.compose.ui.platform

import androidx.compose.ui.internal.getCurrentThreadId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class NativeMainDispatcherTest {
    @Test
    fun startupWorkWaitsForTheHostAndKeepsFifoOrder() {
        val main = NativeMainDispatcher()
        val scope = CoroutineScope(SupervisorJob() + main)
        val dispatcher = NativeUiDispatcher()
        val order = mutableListOf<Int>()
        try {
            scope.launch { order.add(1) }
            scope.launch { order.add(2) }
            assertTrue(order.isEmpty())
            main.attach(dispatcher)
            scope.launch { order.add(3) }
            assertTrue(order.isEmpty())
            dispatcher.runCurrent()
            val expected: List<Int> = [1, 2, 3]
            assertEquals(expected, order)
        }
        finally {
            scope.cancel()
            main.detach(dispatcher)
            dispatcher.close()
        }
    }

    @Test
    fun immediateRunsInlineOnlyOnTheActiveHostThread() = runBlocking {
        val main = NativeMainDispatcher()
        val dispatcher = NativeUiDispatcher()
        val scope = CoroutineScope(SupervisorJob() + main)
        val order = mutableListOf<Int>()
        assertTrue(main.immediate.isDispatchNeeded(EmptyCoroutineContext))
        main.attach(dispatcher)
        try {
            assertFalse(main.immediate.isDispatchNeeded(EmptyCoroutineContext))
            assertSame(main.immediate, main.immediate.immediate)
            assertTrue(withContext(Dispatchers.Default) { main.immediate.isDispatchNeeded(EmptyCoroutineContext) })
            scope.launch {
                order.add(1)
                scope.launch(main.immediate) { order.add(2) }
                scope.launch(main) { order.add(4) }
                order.add(3)
            }
            assertTrue(order.isEmpty())
            dispatcher.runCurrent()
            val expected: List<Int> = [1, 2, 3, 4]
            assertEquals(expected, order)
        }
        finally {
            scope.cancel()
            main.detach(dispatcher)
            dispatcher.close()
        }
        assertTrue(main.immediate.isDispatchNeeded(EmptyCoroutineContext))
    }

    @Test
    fun backgroundSwitchDelayAndTimeoutResumeOnTheHostThread() = runBlocking {
        val main = NativeMainDispatcher()
        val dispatcher = NativeUiDispatcher()
        val hostThread = getCurrentThreadId()
        val wake = Channel<Unit>(Channel.UNLIMITED)
        dispatcher.wake = { wake.trySend(Unit) }
        main.attach(dispatcher)
        try {
            val work = async(Dispatchers.Default) {
                assertNotEquals(hostThread, getCurrentThreadId())
                withContext(main.immediate) {
                    assertEquals(hostThread, getCurrentThreadId())
                    delay(10.milliseconds)
                    assertEquals(hostThread, getCurrentThreadId())
                    assertFailsWith<TimeoutCancellationException> {
                        withTimeout(10.milliseconds) { awaitCancellation() }
                    }
                    assertEquals(hostThread, getCurrentThreadId())
                }
                assertNotEquals(hostThread, getCurrentThreadId())
            }
            work.invokeOnCompletion { wake.trySend(Unit) }
            withTimeout(10_000.milliseconds) {
                while (!work.isCompleted) {
                    wake.receive()
                    dispatcher.runCurrent()
                }
                work.await()
            }
        }
        finally {
            main.detach(dispatcher)
            dispatcher.close()
            wake.close()
        }
    }

    @Test
    fun closedHostCancelsResumingWorkAndStillRunsItsCleanup() = runBlocking {
        val main = NativeMainDispatcher()
        val dispatcher = NativeUiDispatcher()
        val scope = CoroutineScope(SupervisorJob() + main)
        val resume = CompletableDeferred<Unit>()
        val disposed = CompletableDeferred<Unit>()
        var bodyRan = false
        main.attach(dispatcher)
        try {
            val work = scope.launch {
                try {
                    resume.await()
                    bodyRan = true
                }
                finally { disposed.complete(Unit) }
            }
            dispatcher.runCurrent()
            main.detach(dispatcher)
            dispatcher.close()
            resume.complete(Unit)
            withTimeout(10_000.milliseconds) {
                work.join()
                disposed.await()
            }
            assertTrue(work.isCancelled)
            assertFalse(bodyRan)
        }
        finally {
            scope.cancel()
            main.detach(dispatcher)
            dispatcher.close()
        }
    }

    @Test
    fun anExistingScopeUsesTheReplacementHost() {
        val main = NativeMainDispatcher()
        val scope = CoroutineScope(SupervisorJob() + main)
        val first = NativeUiDispatcher()
        val second = NativeUiDispatcher()
        main.attach(first)
        try {
            assertFailsWith<IllegalStateException> { main.attach(second) }
            main.detach(second)
            assertFalse(main.immediate.isDispatchNeeded(EmptyCoroutineContext))
            main.detach(first)
            first.close()
            main.attach(second)
            var ran = false
            scope.launch { ran = true }
            assertFalse(ran)
            second.runCurrent()
            assertTrue(ran)
        }
        finally {
            scope.cancel()
            main.detach(first)
            main.detach(second)
            first.close()
            second.close()
        }
    }

    @Test
    fun aMainScopeCreatedBeforeClaimingTheHostUsesTheInstalledDispatcher() {
        val scope = MainScope()
        val captured = scope.coroutineContext[ContinuationInterceptor]
        val dispatcher = nativeUiDispatcher().claim()
        try {
            if (nativeMainDispatcher != null) {
                assertSame(nativeMainDispatcher, captured)
                assertSame(captured, Dispatchers.Main)
                var ran = false
                scope.launch {
                    assertTrue(dispatcher.isDispatchThread())
                    ran = true
                }
                assertFalse(ran)
                dispatcher.runCurrent()
                assertTrue(ran)
            }
            else assertSame(captured, Dispatchers.Main)
        }
        finally {
            scope.cancel()
            dispatcher.close()
        }
    }
}
