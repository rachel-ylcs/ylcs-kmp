@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.foundation.picker

import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.concurrent.Lock
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.fs.File
import love.yinlin.fs.safeSources
import love.yinlin.io.Sources
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri
import love.yinlin.foundation.picker.internal.*
import love.yinlin.foundation.picker.internal.SDL_FileDialogType.*

actual class Picker actual constructor(private val context: PlatformContextProvider) : AutoCloseable {
    companion object {
        private val pickerCallback = staticCFunction { data: COpaquePointer?, list: CPointer<CPointerVar<ByteVar>>?, _: Int ->
            data?.asStableRef<NativePickerRequest>()?.get()?.complete(list) ?: Unit
        }
    }

    private val lock = Lock()
    private val dialogMutex = Mutex()
    private val pending: MutableSet<NativePickerRequest> = []
    private var closed = false

    private fun window(): COpaquePointer? = context.windowHandle?.let { if (it == 0L) null else RachelPicker_FindWindow(it.toULong()) }

    private fun filterPattern(filter: List<String>): String {
        val extensions = filter.flatMap { it.split(';', ',') }.map { it.trim() }.filter { it.isNotEmpty() }
        return extensions.joinToString(";") { if (it == "*.*") "*" else it.removePrefix("*.").removePrefix(".") }.ifEmpty { "*" }
    }

    private suspend fun paths(filter: List<String>, many: Boolean = false, filename: String? = null): List<String>? = dialogMutex.withLock {
        Coroutines.main {
            suspendCancellableCoroutine { continuation ->
                val request = NativePickerRequest(continuation, filterPattern(filter)) { completed ->
                    lock.synchronized { pending.remove(completed) }
                }
                val active = lock.synchronized { if (closed) false else pending.add(request) }
                if (!active) {
                    request.fail(IllegalStateException("Picker is closed"))
                    return@suspendCancellableCoroutine
                }

                catchingError {
                    if (!continuation.isActive) {
                        request.fail(CancellationException("Picker request was cancelled"))
                        return@suspendCancellableCoroutine
                    }
                    val owner = window()
                    val properties = SDL_CreateProperties()
                    check(properties != 0U) { "Cannot create file dialog properties" }

                    cleaning({
                        SDL_SetPointerProperty(properties, SDL_PROP_FILE_DIALOG_WINDOW_POINTER, owner)
                        SDL_SetPointerProperty(properties, SDL_PROP_FILE_DIALOG_FILTERS_POINTER, request.filters)
                        SDL_SetNumberProperty(properties, SDL_PROP_FILE_DIALOG_NFILTERS_NUMBER, 1L)
                        SDL_SetBooleanProperty(properties, SDL_PROP_FILE_DIALOG_MANY_BOOLEAN, many)
                        if (filename != null) SDL_SetStringProperty(properties, SDL_PROP_FILE_DIALOG_LOCATION_STRING, filename)
                        SDL_ShowFileDialogWithProperties(if (filename == null) SDL_FILEDIALOG_OPENFILE else SDL_FILEDIALOG_SAVEFILE, pickerCallback, request.userdata, properties)
                    }) {
                        SDL_DestroyProperties(properties)
                    }
                }?.let(request::fail)
            }
        }
    }

    actual suspend fun pickPicture(): Source? {
        val path = paths(["jpg;jpeg;png;webp;gif;bmp;avif"])?.firstOrNull() ?: return null
        return pickerResource { File(path).bufferedSource() }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? {
        require(maxNum > 0)
        val paths = paths(["jpg;jpeg;png;webp;gif;bmp;avif"], many = maxNum > 1) ?: return null
        require(paths.size <= maxNum) { "Too many selected pictures" }
        return pickerResource { paths.map { File(it) }.safeSources() }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? {
        val path = paths(filter)?.firstOrNull() ?: return null
        return pickerResource { File(path).bufferedSource() }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = paths(filter)?.firstOrNull()?.let(::RegularUri)

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? = paths([filter], filename = filename)?.firstOrNull()?.let(::RegularUri)

    private suspend fun prepareSave(filename: String, filter: String): Pair<Any, Sink>? {
        val path = paths([filter], filename = filename)?.firstOrNull()?.let { File(it) } ?: return null
        val sink = pickerResource { path.bufferedSink() } ?: return null
        return path to sink
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = prepareSave(filename, "*.webp")

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = prepareSave(filename, "*.mp4")

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Unit

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) (origin as File).delete()
    }

    actual override fun close() {
        val requests = lock.synchronized {
            closed = true
            pending.toList()
        }

        for (request in requests) {
            request.cancel()
        }
    }
}
