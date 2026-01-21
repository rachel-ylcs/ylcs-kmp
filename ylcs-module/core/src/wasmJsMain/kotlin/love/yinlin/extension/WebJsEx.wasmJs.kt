@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.extension

actual val Boolean.raw: JsBoolean get() = this.toJsBoolean()
actual val Double.raw: JsNumber get() = this.toJsNumber()
actual val Long.raw: JsBigInt get() = this.toJsBigInt()
actual val String.raw: JsString get() = this.toJsString()

actual val JsBoolean.cast: Boolean get() = this.toBoolean()
actual val JsNumber.cast: Double get() = this.toDouble()
actual val JsBigInt.cast: Long get() = this.toLong()
actual val JsString.cast: String get() = this.toString()

actual inline fun <reified T : JsAny, reified R> JsArray<out JsAny>.asArray(block: (T) -> R): Array<R> = Array(this.length) { block(this[it] as T) }
actual val JsArray<JsNumber>.asShortArray: ShortArray get() = ShortArray(this.length) { it.toShort() }
actual val JsArray<JsNumber>.asIntArray: IntArray get() = IntArray(this.length) { it }
actual val JsArray<JsNumber>.asFloatArray: FloatArray get() = FloatArray(this.length) { it.toFloat() }
actual val JsArray<JsNumber>.asLongArray: LongArray get() = LongArray(this.length) { it.toLong() }
actual val JsArray<JsNumber>.asDoubleArray: DoubleArray get() = DoubleArray(this.length) { it.toDouble() }
actual val JsArray<JsString>.asArray: Array<String> get() = Array(this.length) { this[it]?.toString() ?: "" }

actual fun jsArrayOf(vararg value: JsAny): JsArray<JsAny> {
    val arr = JsArray<JsAny>()
    for (index in value.indices) arr[index] = value[index]
    return arr
}

actual inline fun <T, R : JsAny> jsArrayOf(vararg value: T, block: (T) -> R): JsArray<JsAny> {
    val dst = JsArray<JsAny>()
    for (index in value.indices) dst[index] = block(value[index])
    return dst
}