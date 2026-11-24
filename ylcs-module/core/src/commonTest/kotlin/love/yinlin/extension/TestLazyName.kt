package love.yinlin.extension

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestLazyName {
    private val symbol = "value"
    private val otherSymbol = "other value"

    @Test
    fun cachesValueWithFirstPropertyName() {
        var initializations = 0
        val delegate = lazyName { name ->
            initializations++
            name
        }

        assertEquals(0, initializations)
        repeat(3) { assertEquals("symbol", delegate.getValue(this, this::symbol)) }
        assertEquals("symbol", delegate.getValue(this, this::otherSymbol))
        assertEquals(1, initializations)
    }

    @Test
    fun retriesAfterInitializationFailure() {
        var attempts = 0
        val resolved by lazyName { name ->
            attempts++
            check(attempts > 1)
            name
        }

        assertFailsWith<IllegalStateException> { resolved }
        assertEquals("resolved", resolved)
        assertEquals("resolved", resolved)
        assertEquals(2, attempts)
    }
}
