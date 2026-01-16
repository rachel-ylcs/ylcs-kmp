@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compatible

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asDeferred as coroutinesAsDeferred
import kotlinx.coroutines.asPromise as coroutinesAsPromise
import kotlinx.coroutines.promise as coroutinesPromise
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.await as coroutinesAwait
import kotlin.js.Promise

actual fun <T> CoroutineScope.promise(
    context: CoroutineContext,
    start: CoroutineStart,
    block: suspend CoroutineScope.() -> T
): Promise<JsAny?> = this.coroutinesPromise(context, start, block)

actual fun <T> Deferred<T>.asPromise(): Promise<JsAny?> = this.coroutinesAsPromise()

actual fun <T> Promise<JsAny?>.asDeferred(): Deferred<T> = this.coroutinesAsDeferred()

actual suspend fun <T : JsAny?> Promise<T>.await(): T = this.coroutinesAwait()