@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.extension

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import kotlin.js.*

fun <T : JsAny> JsAny.cast() = unsafeCast<T>()

expect val Boolean.raw: JsBoolean
expect val Double.raw: JsNumber
expect val Long.raw: JsBigInt
expect val String.raw: JsString

expect val JsBoolean.cast: Boolean
expect val JsNumber.cast: Double
expect val JsBigInt.cast: Long
expect val JsString.cast: String

expect inline fun <reified T : JsAny, reified R> JsArray<out JsAny>.asArray(block: (T) -> R): Array<R>

private fun JsArray<JsNumber>.numberAt(index: Int): Double = requireNotNull(this[index]) { "Missing number at index $index" }.cast

val JsArray<JsNumber>.asShortArray: ShortArray get() = ShortArray(length) { index -> numberAt(index).toInt().toShort() }
val JsArray<JsNumber>.asIntArray: IntArray get() = IntArray(length) { index -> numberAt(index).toInt() }
val JsArray<JsNumber>.asFloatArray: FloatArray get() = FloatArray(length) { index -> numberAt(index).toFloat() }
val JsArray<JsNumber>.asLongArray: LongArray get() = LongArray(length) { index -> numberAt(index).toLong() }
val JsArray<JsNumber>.asDoubleArray: DoubleArray get() = DoubleArray(length) { index -> numberAt(index) }
expect val JsArray<JsString>.asArray: Array<String>

expect fun jsArrayOf(vararg value: JsAny): JsArray<JsAny>
expect inline fun <T, R : JsAny> jsArrayOf(vararg value: T, block: (T) -> R): JsArray<JsAny>

fun jsArrayOf(vararg value: Number): JsArray<JsAny> = jsArrayOf<Number, JsAny>(*value) { it.toDouble().raw }
fun jsArrayOf(vararg value: String): JsArray<JsAny> = jsArrayOf<String, JsAny>(*value) { it.raw }

val ArrayBuffer.asByteArray: ByteArray get() = Int8Array(this).toByteArray()
