@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.extension

import love.yinlin.compatible.WebByteArray
import org.khronos.webgl.ArrayBuffer
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
expect val JsArray<JsNumber>.asArray: Array<Double>
expect val JsArray<JsString>.asArray: Array<String>

expect fun jsArrayOf(vararg value: JsAny): JsArray<JsAny>
expect inline fun <T, R : JsAny> jsArrayOf(vararg value: T, block: (T) -> R): JsArray<JsAny>

fun jsArrayOf(vararg value: Number): JsArray<JsAny> = jsArrayOf<Number, JsAny>(*value) { it.toDouble().raw }
fun jsArrayOf(vararg value: String): JsArray<JsAny> = jsArrayOf<String, JsAny>(*value) { it.raw }

val ArrayBuffer.asByteArray: ByteArray get() = WebByteArray(this).toByteArray()