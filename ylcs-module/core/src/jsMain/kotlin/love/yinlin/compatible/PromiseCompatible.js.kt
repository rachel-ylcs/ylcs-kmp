@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compatible

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import love.yinlin.platform.unsupportedPlatform
import kotlin.coroutines.CoroutineContext
import kotlin.js.Promise

actual fun <T> CoroutineScope.promise(
    context: CoroutineContext,
    start: CoroutineStart,
    block: suspend CoroutineScope.() -> T
): Promise<JsAny?> = unsupportedPlatform()

actual fun <T> Deferred<T>.asPromise(): Promise<JsAny?> = unsupportedPlatform()

actual fun <T> Promise<JsAny?>.asDeferred(): Deferred<T> = unsupportedPlatform()

actual suspend fun <T : JsAny?> Promise<T>.await(): T = unsupportedPlatform()