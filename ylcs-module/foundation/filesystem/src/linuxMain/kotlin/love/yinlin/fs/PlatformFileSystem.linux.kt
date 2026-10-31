package love.yinlin.fs

import kotlinx.cinterop.*
import love.yinlin.foundation.PlatformContext
import platform.posix.W_OK
import platform.posix.access
import platform.posix.chdir
import platform.posix.free
import platform.posix.getcwd
import platform.posix.getenv

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"

    @OptIn(ExperimentalForeignApi::class)
    actual fun appPath(context: PlatformContext, appName: String): File {
        val workingDir = getCurrentDirectory()
        if (workingDir != null && access(workingDir.path, W_OK) == 0) return workingDir

        val dataHome = getenv("XDG_DATA_HOME")?.toKString()?.takeIf { it.startsWith('/') }
        if (dataHome != null) return File(dataHome, appName)

        val homeDir = checkNotNull(getenv("HOME")?.toKString()?.takeIf { it.isNotBlank() }) { "Cannot locate the home directory" }
        return File(homeDir, ".local", "share", appName)
    }

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