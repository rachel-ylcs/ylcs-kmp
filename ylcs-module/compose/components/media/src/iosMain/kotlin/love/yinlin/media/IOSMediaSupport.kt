@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
package love.yinlin.media

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import love.yinlin.concurrent.atomic
import love.yinlin.coroutines.mainContext
import platform.CoreMedia.*
import platform.Foundation.*

/**
 * 每个播放器拥有自己的生命周期和 scope。关闭请求从任意线程立即生效；
 * 取消排队任务后，仅允许一次 Main 线程清理继续执行。
 */
internal class IOSPlayerLifetime {
    private val closing = atomic(false)
    val scope = CoroutineScope(SupervisorJob() + mainContext)
    val isClosed: Boolean get() = closing.value

    fun close(cleanup: () -> Unit) {
        if (!closing.compareAndSet(expect = false, update = true)) return
        scope.cancel()
        scope.launch(Dispatchers.Main.immediate) {
            cleanup()
        }
    }
}

internal fun iosMediaURL(path: String): NSURL? = when {
    path.isBlank() -> null
    path.startsWith("https://", ignoreCase = true) || path.startsWith("http://", ignoreCase = true) -> NSURL.URLWithString(path)?.takeIf { !it.host.isNullOrBlank() }
    path.startsWith("file:", ignoreCase = true) -> NSURL.URLWithString(path)
    else -> NSURL.fileURLWithPath(path)
}

internal fun CValue<CMTime>.iosMilliseconds(): Long {
    val seconds = CMTimeGetSeconds(this)
    return if (seconds.isFinite() && seconds >= 0.0) (seconds * 1000.0).toLong() else 0L
}

internal fun iosError(message: String, error: NSError?): Throwable = IllegalStateException(if (error == null) message else "$message: ${error.localizedDescription} (${error.domain}, ${error.code})")

internal fun iosCheck(message: String, operation: (CPointer<ObjCObjectVar<NSError?>>) -> Boolean) {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        error.value = null
        if (!operation(error.ptr)) throw iosError(message, error.value)
    }
}

internal fun Any?.iosUnsigned(): ULong? = when (this) {
    is NSNumber -> unsignedLongLongValue
    is Number -> toLong().toULong()
    else -> null
}