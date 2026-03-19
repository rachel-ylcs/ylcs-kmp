package love.yinlin.fs

import love.yinlin.foundation.PlatformContext

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '\\'
    actual val LineSeparator: String = "\r\n"

    actual fun appPath(context: PlatformContext, appName: String): File = File(StandardPath.Running.path).parent!!

    actual fun dataPath(context: PlatformContext, appName: String): File = File(appPath(context, appName), "data")

    actual fun cachePath(context: PlatformContext, appName: String): File = File(StandardPath.Temp.path, appName, "temp")
}