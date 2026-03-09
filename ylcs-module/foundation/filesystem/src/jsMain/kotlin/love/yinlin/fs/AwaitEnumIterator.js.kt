package love.yinlin.fs

import love.yinlin.platform.unsupportedPlatform
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
actual fun <R : JsAny?> awaitEnumIterator(value: JsAny): Promise<JsArray<R>> = unsupportedPlatform()