@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.picker

import kotlinx.cinterop.*
import kotlinx.coroutines.CancellableContinuation
import love.yinlin.concurrent.Lock
import love.yinlin.picker.internal.*
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

    fun complete(filelist: CPointer<CPointerVar<ByteVar>>?) {
        if (!lock.synchronized {
            if (finished) false
            else {
                finished = true
                true
            }
        }) return
        try {
            val result = runCatching {
                checkNotNull(filelist) { SDL_GetError()?.toKString() ?: "File dialog failed" }
                buildList {
                    var index = 0
                    while (true) {
                        val path = filelist[index++] ?: break
                        add(path.toKString())
                    }
                }.takeIf { it.isNotEmpty() }
            }
            if (continuation.isActive) result.fold({ continuation.resume(it) }, { continuation.resumeWithException(it) })
        }
        finally {
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
        try {
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        finally {
            onFinished(this)
            arena.clear()
            reference.dispose()
        }
    }
}

internal val pickerCallback = staticCFunction { userdata: COpaquePointer?, filelist: CPointer<CPointerVar<ByteVar>>?, _: Int ->
    if (userdata != null) userdata.asStableRef<NativePickerRequest>().get().complete(filelist)
}
