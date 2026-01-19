package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@CompatibleRachelApi
expect class ThrowableCompatible(value: JsAny?) {
    fun build(): Throwable?
}