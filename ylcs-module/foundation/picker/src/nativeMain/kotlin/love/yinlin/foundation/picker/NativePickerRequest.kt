@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.foundation.picker

import kotlinx.cinterop.*
import kotlinx.coroutines.CancellableContinuation
import love.yinlin.concurrent.Lock
import love.yinlin.extension.cleaning
import love.yinlin.foundation.picker.internal.SDL_DialogFileFilter
import love.yinlin.foundation.picker.internal.SDL_GetError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class NativePickerRequest(
    private val continuation: CancellableContinuation<List<String>?>,
    filter: String,
    private val onFinished: (NativePickerRequest) -> Unit
) {
    private val lock = Lock()
    private var finished = false
    private val arena = Arena()
    val filters: CPointer<SDL_DialogFileFilter> = arena.alloc<SDL_DialogFileFilter>().apply {
        name = "Files".cstr.getPointer(arena)
        pattern = filter.cstr.getPointer(arena)
    }.ptr
    private val reference = StableRef.create(this)
    val userdata: COpaquePointer = reference.asCPointer()

    fun cancel() = continuation.cancel()

    fun complete(list: CPointer<CPointerVar<ByteVar>>?) {
        if (!lock.synchronized {
            if (finished) false
            else {
                finished = true
                true
            }
        }) return

        cleaning({
            val result = runCatching {
                checkNotNull(list) { SDL_GetError()?.toKString() ?: "File dialog failed" }
                buildList {
                    var index = 0
                    while (true) {
                        val path = list[index++] ?: break
                        add(path.toKString())
                    }
                }.takeIf { it.isNotEmpty() }
            }

            if (continuation.isActive) {
                result.fold({ continuation.resume(it) }, { continuation.resumeWithException(it) })
            }
        }) {
            onFinished(this)
            arena.clear()
            reference.dispose()
        }
    }

    fun fail(error: Throwable) {
        if (!lock.synchronized {
            if (finished) false
            else {
                finished = true
                true
            }
        }) return

        cleaning({
            if (continuation.isActive) continuation.resumeWithException(error)
        }) {
            onFinished(this)
            arena.clear()
            reference.dispose()
        }
    }
}