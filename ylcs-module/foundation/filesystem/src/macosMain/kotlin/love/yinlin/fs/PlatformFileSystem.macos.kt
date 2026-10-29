package love.yinlin.fs

import love.yinlin.foundation.PlatformContext
import platform.Foundation.*
import platform.posix.chdir

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"

    actual fun appPath(context: PlatformContext, appName: String): File = checkNotNull(getCurrentDirectory()) { "Cannot read the current working directory" }

    actual fun dataPath(context: PlatformContext, appName: String): File = File(appPath(context, appName), "data")

    actual fun cachePath(context: PlatformContext, appName: String): File {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        return File((paths.firstOrNull() as? String) ?: "", appName, "temp")
    }

    actual fun getCurrentDirectory(): File? = NSFileManager.defaultManager.currentDirectoryPath.takeIf { it.isNotEmpty() }?.let(File::invoke)

    actual fun setCurrentDirectory(path: File): Boolean = chdir(path.path) == 0
}