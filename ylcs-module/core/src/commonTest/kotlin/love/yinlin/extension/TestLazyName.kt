package love.yinlin.extension

import kotlin.reflect.KProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestLazyName {
    private val symbol = "value"

    @Test
    fun readsPropertyNameOnlyDuringInitialization() {
        var nameReads = 0
        val property = object : KProperty<String> by this::symbol {
            override val name: String
                get() {
                    nameReads++
                    return "symbol"
                }
        }
        val delegate = lazyName { it }

        repeat(3) { assertEquals("symbol", delegate.getValue(this, property)) }
        assertEquals(1, nameReads)
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
