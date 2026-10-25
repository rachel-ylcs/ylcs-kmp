package love.yinlin.nativecompose.ui

import androidx.compose.ui.util.doubleFromBits
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.floatFromBits
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.trace
import androidx.compose.ui.util.traceValue
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class UiUtilTest {
    @Test
    fun packingPreservesSignsAndRawBits() {
        val integers = packInts(Int.MIN_VALUE, Int.MAX_VALUE)
        assertEquals(Int.MIN_VALUE, unpackInt1(integers))
        assertEquals(Int.MAX_VALUE, unpackInt2(integers))
        val floats = packFloats(-0f, floatFromBits(0x7fc00001))
        assertEquals(Int.MIN_VALUE, unpackFloat1(floats).toRawBits())
        assertEquals(0x7fc00001, unpackFloat2(floats).toRawBits())
        assertEquals(Long.MIN_VALUE, doubleFromBits(Long.MIN_VALUE).toRawBits())
    }

    @Test
    fun roundingHandlesTiesNanAndOverflow() {
        assertEquals(2, 1.5f.fastRoundToInt())
        assertEquals(-1, (-1.5f).fastRoundToInt())
        assertEquals(0, Float.NaN.fastRoundToInt())
        assertEquals(0, Double.NaN.fastRoundToInt())
        assertEquals(Int.MAX_VALUE, Double.POSITIVE_INFINITY.fastRoundToInt())
        assertEquals(Int.MIN_VALUE, Float.NEGATIVE_INFINITY.fastRoundToInt())
    }

    @Test
    fun distinctFilteringUsesTheRealScatterCollectionImplementation() {
        val values: List<String> = ["alpha", "beta", "alpha", "gamma", "beta"]
        val expected: List<String> = ["alpha", "beta", "gamma"]
        assertEquals(expected, values.fastDistinctBy { it })
    }

    @Test
    fun tracingExecutesTheBlockExactlyOnceAndReturnsItsValue() {
        var calls = 0
        val marker = Any()
        assertSame(marker, trace("native-ui") {
            calls++
            marker
        })
        traceValue("native-ui", Long.MAX_VALUE)
        assertEquals(1, calls)
    }

    @Test
    fun tracingPreservesTheOriginalException() {
        val failure = IllegalStateException("trace fixture")
        var caught: Throwable? = null
        try { trace("native-ui") { throw failure } }
        catch (error: Throwable) { caught = error }
        assertSame(failure, caught)
    }
}
