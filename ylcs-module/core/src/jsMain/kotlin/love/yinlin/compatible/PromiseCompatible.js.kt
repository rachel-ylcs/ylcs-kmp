@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compatible

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.platform.unsupportedPlatform
import kotlin.coroutines.CoroutineContext
import kotlin.js.Promise

@CompatibleRachelApi
actual fun <T> CoroutineScope.promise(
    context: CoroutineContext,
    start: CoroutineStart,
    block: suspend CoroutineScope.() -> T
): Promise<JsAny?> = unsupportedPlatform()

@CompatibleRachelApi
actual fun <T> Deferred<T>.asPromise(): Promise<JsAny?> = unsupportedPlatform()

@CompatibleRachelApi
actual fun <T> Promise<JsAny?>.asDeferred(): Deferred<T> = unsupportedPlatform()

@CompatibleRachelApi
actual suspend fun <T : JsAny?> Promise<T>.await(): T = unsupportedPlatform()