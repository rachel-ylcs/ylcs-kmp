package love.yinlin.fs

import kotlinx.cinterop.*
import love.yinlin.foundation.PlatformContextDelegate
import platform.posix.getcwd
import platform.posix.getenv

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"

    @OptIn(ExperimentalForeignApi::class)
    actual fun appPath(context: PlatformContextDelegate, appName: String): File = File(memScoped {
        val buffer = allocArray<ByteVar>(1024)
        getcwd(buffer, 1024UL)
        buffer.toKString()
    })

    actual fun dataPath(context: PlatformContextDelegate, appName: String): File = File(appPath(context, appName), "data")

    @OptIn(ExperimentalForeignApi::class)
    actual fun cachePath(context: PlatformContextDelegate, appName: String): File {
        val xdgCache = getenv("XDG_CACHE_HOME")?.toKString()
        if (!xdgCache.isNullOrBlank()) return File(xdgCache, appName, "temp")
        val home = getenv("HOME")?.toKString()
        if (!home.isNullOrBlank()) return File(home, ".cache", appName, "temp")
        return File("/tmp", appName, "temp")
    }
}