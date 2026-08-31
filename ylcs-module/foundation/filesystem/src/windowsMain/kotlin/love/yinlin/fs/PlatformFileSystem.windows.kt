package love.yinlin.fs

import love.yinlin.foundation.PlatformContext
import platform.windows.SetCurrentDirectoryW

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '\\'
    actual val LineSeparator: String = "\r\n"

    actual fun appPath(context: PlatformContext, appName: String): File = File(StandardPath.Running.path).parent!!

    actual fun dataPath(context: PlatformContext, appName: String): File = File(appPath(context, appName), "data")

    actual fun cachePath(context: PlatformContext, appName: String): File = File(StandardPath.Temp.path, appName, "temp")

    actual fun setCurrentDirectory(path: File): Boolean = SetCurrentDirectoryW(path.path) != 0
}