package love.yinlin.coroutines

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import love.yinlin.extension.cleaning
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue

class TestSyncFuture {
    @Test
    @Suppress("BlockingMethodInNonBlockingContext")
    fun concurrentCallbacksCompleteOnlyOnce() = runTest {
        Coroutines.io {
            val executor = Executors.newFixedThreadPool(8)

            cleaning({
                repeat(200) {
                    val barrier = CyclicBarrier(8)
                    val callbacks = mutableListOf<Future<*>>()
                    val result = Coroutines.sync { future ->
                        repeat(8) { value ->
                            callbacks += executor.submit {
                                barrier.await(10, TimeUnit.SECONDS)
                                future.send(value)
                            }
                        }
                    }

                    for (callback in callbacks) callback.get(10, TimeUnit.SECONDS)
                    assertTrue(result in 0 ..< 8)
                }
            }, executor::shutdownNow)
        }
    }

    @Test
    fun lateCallbacksPreserveCancellation() = runTest {
        lateinit var future: SyncFuture<Int>
        val task = async(start = CoroutineStart.UNDISPATCHED) {
            Coroutines.sync { future = it }
        }
        task.cancel()
        future.send(1)
        future.send()
        task.join()
        assertTrue(task.isCancelled)
    }
}