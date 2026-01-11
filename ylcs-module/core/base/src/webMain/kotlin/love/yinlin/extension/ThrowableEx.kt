@file:OptIn(ExperimentalWasmJsInterop::class)

package love.yinlin.extension

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js

private fun throwJs(e: JsAny): Nothing = js("{ throw e; }")

@Suppress("RedundantNullableReturnType")
private fun catchJs(f: () -> Unit): JsAny? = js("""
{
    try {
        f();
        return null;
    } catch (e) {
       return e;
    }
}
""")

val JsAny.asThrowableOrNull: Throwable? get() {
    val thisAny: Any = this
    if (thisAny is Throwable) return thisAny
    var result: Throwable? = null
    catchJs {
        try {
            throwJs(this)
        } catch (e: Throwable) {
            result = e
        }
    }
    return result
}