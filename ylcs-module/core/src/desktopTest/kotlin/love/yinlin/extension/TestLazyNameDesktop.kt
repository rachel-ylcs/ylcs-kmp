package love.yinlin.extension

import kotlin.reflect.KProperty
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLazyNameDesktop {
    private val symbol = "value"

    // Kotlin/JS property references use compiler-generated metadata, so the
    // custom KProperty getter is only used to count reads in the JVM test.
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
}
