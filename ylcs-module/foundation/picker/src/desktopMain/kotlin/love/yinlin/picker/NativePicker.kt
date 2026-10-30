package love.yinlin.picker

import love.yinlin.annotation.NativeLibApi
import love.yinlin.platform.NativeLibLoader
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.Executors

@NativeLibApi
internal object NativePicker {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "RachelFilePicker").apply { isDaemon = true }
    }

    suspend fun <T> run(block: () -> T): T = suspendCancellableCoroutine { continuation ->
        val operation = executor.submit {
            try {
                continuation.resume(block())
            }
            catch (error: Throwable) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
        continuation.invokeOnCancellation { operation.cancel(false) }
    }

    init {
        NativeLibLoader.resource("ylcs-module.foundation.picker")
    }

    @JvmStatic
    external fun openFileDialog(parent: Long, title: String, filterName: String, filter: String): String?
    @JvmStatic
    external fun openMultipleFileDialog(parent: Long, maxNum: Int, title: String, filterName: String, filter: String): Array<String>
    @JvmStatic
    external fun saveFileDialog(parent: Long, title: String, filename: String, ext: String, filterName: String): String?
}