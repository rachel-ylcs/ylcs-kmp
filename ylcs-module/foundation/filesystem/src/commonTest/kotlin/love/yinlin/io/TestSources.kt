package love.yinlin.io

import kotlinx.coroutines.CancellationException
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class TestSources {
    private class RecordingSource(private val failure: Throwable? = null) : RawSource {
        var closed = 0
        override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = -1
        override fun close() {
            closed++
            if (failure != null) throw failure
        }
    }

    private fun stopDuringMapping(source: RecordingSource): Boolean {
        [1, 2].safeToSources {
            if (it == 2) return false
            source
        }
        return true
    }

    @Test
    fun nonLocalReturnStillClosesOpenedSources() {
        val source = RecordingSource()
        assertEquals(false, stopDuringMapping(source))
        assertEquals(1, source.closed)
    }

    @Test
    fun closeAttemptsEverySourceAndPreservesSuppressedErrors() {
        val firstError = IllegalStateException("first close")
        val secondError = IllegalArgumentException("second close")
        val first = RecordingSource(firstError)
        val second = RecordingSource(secondError)
        val third = RecordingSource()
        val sources = Sources(mutableListOf(first, second, third))
        val error = assertFailsWith<IllegalStateException> { sources.close() }
        assertSame(firstError, error)
        assertSame(secondError, error.suppressedExceptions.single())
        assertEquals([1, 1, 1], [first.closed, second.closed, third.closed])
    }

    @Test
    fun mappingFailureClosesAlreadyOpenedSources() {
        val source = RecordingSource()
        val result = [1, 2].safeToSources {
            if (it == 2) error("fixture fetch failed")
            source
        }
        assertNull(result)
        assertEquals(1, source.closed)
    }

    @Test
    fun successfulMappingTransfersOwnershipAndSkipsNullEntries() {
        val first = RecordingSource()
        val second = RecordingSource()
        val result = assertNotNull([1, 2, 3].safeToSources {
            when (it) {
                1 -> first
                2 -> null
                else -> second
            }
        })
        assertEquals(2, result.size)
        assertSame(first, result[0])
        assertSame(second, result[1])
        assertEquals([0, 0], [first.closed, second.closed])
        result.close()
        assertEquals([1, 1], [first.closed, second.closed])
    }

    @Test
    fun cancellationClosesResourcesAndRemainsPrimaryFailure() {
        val cancelled = CancellationException("fixture cancellation")
        val closeError = IllegalStateException("fixture close")
        val first = RecordingSource(closeError)
        val second = RecordingSource()
        val error = assertFailsWith<CancellationException> {
            [1, 2, 3].safeToSources {
                when (it) {
                    1 -> first
                    2 -> second
                    else -> throw cancelled
                }
            }
        }
        assertSame(cancelled, error)
        assertSame(closeError, error.suppressedExceptions.single())
        assertEquals([1, 1], [first.closed, second.closed])
    }
}