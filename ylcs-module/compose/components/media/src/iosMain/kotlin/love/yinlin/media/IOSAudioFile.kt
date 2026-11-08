@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package love.yinlin.media

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import love.yinlin.coroutines.ioContext
import love.yinlin.extension.catching
import love.yinlin.extension.catchingError
import love.yinlin.fs.File
import platform.Foundation.*
import kotlin.coroutines.resumeWithException

internal suspend fun <T> withIOSAudioFile(path: String, load: suspend (String) -> T): T {
    require('\u0000' !in path) { "无效的音频地址" }
    val url = requireNotNull(iosMediaURL(path)) { "无效的音频地址" }
    if (url.isFileURL()) return load(requireNotNull(url.path))

    // NSURLSession owns its download URL until the callback returns. The decoder keeps its file open after load.
    val file = downloadIOSAudio(url)
    try { return load(file.path) }
    finally { withContext(NonCancellable + ioContext) { catching { file.deleteSync() } } }
}

private suspend fun downloadIOSAudio(url: NSURL): File = suspendCancellableCoroutine { continuation ->
    val task = NSURLSession.sharedSession.downloadTaskWithURL(url) { location, response, error ->
        if (error != null) {
            continuation.resumeWithException(iosError("下载音频失败", error))
            return@downloadTaskWithURL
        }
        val destination = File(NSTemporaryDirectory(), NSUUID().UUIDString)
        val failure = catchingError {
            val status = (response as? NSHTTPURLResponse)?.statusCode ?: 0L
            check(location != null && status in 200L .. 299L) { "下载音频失败 ($status)" }
            iosCheck("保存下载的音频失败") {
                NSFileManager.defaultManager.moveItemAtURL(location, NSURL.fileURLWithPath(destination.path), it)
            }
        }
        if (failure != null) continuation.resumeWithException(failure)
        else continuation.resume(destination) { _, file, _ -> catching { file.deleteSync() } }
    }
    continuation.invokeOnCancellation { task.cancel() }
    task.resume()
}
