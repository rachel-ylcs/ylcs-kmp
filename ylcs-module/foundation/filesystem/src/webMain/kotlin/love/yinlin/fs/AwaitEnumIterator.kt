package love.yinlin.fs

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
expect fun <R : JsAny?> awaitEnumIterator(value: JsAny): Promise<JsArray<R>>