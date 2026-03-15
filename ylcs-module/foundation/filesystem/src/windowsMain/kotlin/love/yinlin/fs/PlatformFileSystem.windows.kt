package love.yinlin.fs

import love.yinlin.foundation.PlatformContextDelegate

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '\\'
    actual val LineSeparator: String = "\r\n"

    actual fun appPath(context: PlatformContextDelegate, appName: String): File = File(StandardPath.Running.path).parent!!

    actual fun dataPath(context: PlatformContextDelegate, appName: String): File = File(appPath(context, appName), "data")

    actual fun cachePath(context: PlatformContextDelegate, appName: String): File = File(StandardPath.Temp.path, appName, "temp")
}