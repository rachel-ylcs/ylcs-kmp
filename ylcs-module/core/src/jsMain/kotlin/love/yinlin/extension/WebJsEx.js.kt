@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.extension

actual val Boolean.raw: JsBoolean get() = this
actual val Double.raw: JsNumber get() = this
actual val Long.raw: JsBigInt get() = this
actual val String.raw: JsString get() = this

actual val JsBoolean.cast: Boolean get() = this
actual val JsNumber.cast: Double get() = this
actual val JsBigInt.cast: Long get() = this
actual val JsString.cast: String get() = this

actual inline fun <reified T : JsAny, reified R> JsArray<out JsAny>.asArray(block: (T) -> R): Array<R> = Array(this.length) { block(this[it] as T) }
actual val JsArray<JsNumber>.asArray: Array<Double> get() = this
actual val JsArray<JsString>.asArray: Array<String> get() = this

actual fun jsArrayOf(vararg value: JsAny): JsArray<JsAny> = arrayOf(*value)

actual inline fun <T, R : JsAny> jsArrayOf(vararg value: T, block: (T) -> R): JsArray<JsAny> = Array(value.size) { block(value[it]) }