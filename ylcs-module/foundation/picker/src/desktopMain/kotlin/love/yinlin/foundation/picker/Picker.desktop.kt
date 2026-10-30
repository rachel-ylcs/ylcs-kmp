package love.yinlin.foundation.picker

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.NativeLibApi
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.fs.File
import love.yinlin.fs.safeSources
import love.yinlin.io.Sources
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri
import java.util.concurrent.Executors

@NativeLibApi
actual class Picker actual constructor(private val context: PlatformContextProvider) : AutoCloseable {
    private val lifetime = Job()

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "RachelFilePicker").apply { isDaemon = true }
    }

    private suspend fun paths(filter: String, filename: String? = null, maxNum: Int = 1): List<String>? = coroutineScope {
        lifetime.ensureActive()
        val registration = lifetime.invokeOnCompletion { cancel() }
        val owner = context.windowHandle ?: 0L

        Coroutines.sync { future ->
            val operation = executor.submit {
                future.send {
                    when {
                        filename != null -> NativePicker.saveFileDialog(owner, "保存文件", filename, filter, "文件")?.let(::listOf)
                        maxNum == 1 -> NativePicker.openFileDialog(owner, "选择文件", "文件", filter)?.let(::listOf)
                        else -> NativePicker.openMultipleFileDialog(owner, maxNum, "选择文件", "文件", filter).toList().takeIf { it.isNotEmpty() }
                    }
                }
                registration.dispose()
            }
            future.clean { operation.cancel(false) }
        }
    }

    actual suspend fun pickPicture(): Source? {
        val path = paths("*.jpg;*.jpeg;*.png;*.webp;*.gif;*.bmp;*.avif")?.firstOrNull() ?: return null
        return pickerResource { File(path).bufferedSource() }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? {
        require(maxNum > 0)
        val paths = paths("*.jpg;*.jpeg;*.png;*.webp;*.gif;*.bmp;*.avif", maxNum = maxNum) ?: return null
        require(paths.size <= maxNum) { "Too many selected pictures" }
        return pickerResource { paths.map { File(it) }.safeSources() }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? {
        val path = paths(filter.joinToString(";"))?.firstOrNull() ?: return null
        return pickerResource { File(path).bufferedSource() }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? =
        paths(filter.joinToString(";"))?.firstOrNull()?.let(::RegularUri)

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? =
        paths(filter, filename = filename)?.firstOrNull()?.let(::RegularUri)

    private suspend fun prepareSave(filename: String, filter: String): Pair<Any, Sink>? {
        val path = paths(filter, filename = filename)?.firstOrNull()?.let { File(it) } ?: return null
        val sink = pickerResource { path.bufferedSink() } ?: return null
        return path to sink
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = prepareSave(filename, "*.webp")

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = prepareSave(filename, "*.mp4")

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Unit

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) (origin as File).delete()
    }

    actual override fun close() = lifetime.cancel()
}
