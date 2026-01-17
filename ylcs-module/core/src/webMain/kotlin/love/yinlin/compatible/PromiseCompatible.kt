@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compatible

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

expect fun <T> CoroutineScope.promise(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Promise<JsAny?>

expect fun <T> Deferred<T>.asPromise(): Promise<JsAny?>

expect fun <T> Promise<JsAny?>.asDeferred(): Deferred<T>

expect suspend fun <T : JsAny?> Promise<T>.await(): T