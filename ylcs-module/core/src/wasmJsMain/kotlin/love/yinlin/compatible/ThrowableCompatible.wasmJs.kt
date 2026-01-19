package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi

@OptIn(ExperimentalWasmJsInterop::class)
@CompatibleRachelApi
actual class ThrowableCompatible actual constructor(private val value: JsAny?) {
    actual fun build(): Throwable? = value?.toThrowableOrNull()
}