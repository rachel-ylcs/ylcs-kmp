package love.yinlin.fs

import love.yinlin.foundation.PlatformContextDelegate

actual object PlatformFileSystem {
    actual val PathSeparator: Char = java.io.File.separatorChar
    actual val LineSeparator: String = System.lineSeparator()
    actual fun appPath(context: PlatformContextDelegate, appName: String): File = File(context.dataDir.absolutePath)
    actual fun dataPath(context: PlatformContextDelegate, appName: String): File = File(context.filesDir.absolutePath)
    actual fun cachePath(context: PlatformContextDelegate, appName: String): File = File(context.cacheDir.absolutePath, "temp")
}