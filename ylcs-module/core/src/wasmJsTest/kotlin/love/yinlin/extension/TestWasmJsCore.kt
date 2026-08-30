@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.extension

import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array
import kotlin.test.Test
import kotlin.test.assertContentEquals

class TestWasmJsCore {
    @Test
    fun testJsArrayOf() {
        val arr1 = jsArrayOf(0.0.raw, "alice".raw, false.raw)
        assertContentEquals(arrayOf(0.0.raw, "alice".raw, false.raw), arr1.toArray())

        val arr2 = jsArrayOf(2, 3.0, 5L)
        assertContentEquals(arrayOf(2.0.raw, 3.0.raw, 5.0.raw), arr2.toArray())

        val arr3 = jsArrayOf("hello", "world")
        assertContentEquals(arrayOf("hello".raw, "world".raw), arr3.toArray())

        val arr4 = jsArrayOf("hello".encodeToByteArray(), "world".encodeToByteArray()) { it.toInt8Array() }
        assertContentEquals(arrayOf("hello", "world"), arr4.asArray { v: Int8Array -> v.toByteArray().decodeToString() })
    }
}