package love.yinlin.fs

import love.yinlin.foundation.PlatformContext
import platform.Foundation.*
import platform.posix.chdir

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"

    private fun searchPath(directory: NSSearchPathDirectory): File {
        val paths = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true)
        return File((paths.firstOrNull() as? String) ?: "")
    }

    actual fun appPath(context: PlatformContext, appName: String): File = File(NSHomeDirectory())

    actual fun dataPath(context: PlatformContext, appName: String): File = searchPath(NSDocumentDirectory)

    actual fun cachePath(context: PlatformContext, appName: String): File = File(searchPath(NSCachesDirectory), "temp")

    actual fun setCurrentDirectory(path: File): Boolean = chdir(path.path) == 0
}