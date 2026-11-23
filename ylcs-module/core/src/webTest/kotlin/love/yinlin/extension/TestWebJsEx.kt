@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.extension

import kotlin.test.Test
import kotlin.test.assertContentEquals

class TestWebJsEx {
    @Test
    fun convertsNumericArrayElements() {
        val numbers = jsArrayOf(42.75, -8.5, 17.0).cast<JsArray<JsNumber>>()

        assertContentEquals([42, -8, 17], numbers.asShortArray)
        assertContentEquals([42, -8, 17], numbers.asIntArray)
        assertContentEquals([42.75f, -8.5f, 17.0f], numbers.asFloatArray)
        assertContentEquals([42L, -8L, 17L], numbers.asLongArray)
        assertContentEquals([42.75, -8.5, 17.0], numbers.asDoubleArray)
    }
}
