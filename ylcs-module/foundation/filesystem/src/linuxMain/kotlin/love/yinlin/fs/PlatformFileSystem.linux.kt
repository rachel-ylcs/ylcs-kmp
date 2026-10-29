package love.yinlin.fs

import kotlinx.cinterop.*
import love.yinlin.foundation.PlatformContext
import platform.posix.chdir
import platform.posix.free
import platform.posix.getcwd
import platform.posix.getenv

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"

    actual fun appPath(context: PlatformContext, appName: String): File = checkNotNull(getCurrentDirectory()) { "Cannot read the current working directory" }

    actual fun dataPath(context: PlatformContext, appName: String): File = File(appPath(context, appName), "data")

    @OptIn(ExperimentalForeignApi::class)
    actual fun cachePath(context: PlatformContext, appName: String): File {
        val xdgCache = getenv("XDG_CACHE_HOME")?.toKString()
        if (!xdgCache.isNullOrBlank()) return File(xdgCache, appName, "temp")
        val home = getenv("HOME")?.toKString()
        if (!home.isNullOrBlank()) return File(home, ".cache", appName, "temp")
        return File("/tmp", appName, "temp")
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun getCurrentDirectory(): File? {
        val buffer = getcwd(null, 0UL) ?: return null
        try { return File(buffer.toKString()) }
        finally { free(buffer) }
    }

    actual fun setCurrentDirectory(path: File): Boolean = chdir(path.path) == 0
}