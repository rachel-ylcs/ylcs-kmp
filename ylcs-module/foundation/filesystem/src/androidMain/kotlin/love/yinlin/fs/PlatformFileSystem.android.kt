package love.yinlin.fs

import love.yinlin.foundation.PlatformContext

actual object PlatformFileSystem {
    actual val PathSeparator: Char = java.io.File.separatorChar
    actual val LineSeparator: String = System.lineSeparator()
    actual fun appPath(context: PlatformContext, appName: String): File = File(context.dataDir.absolutePath)
    actual fun dataPath(context: PlatformContext, appName: String): File = File(context.filesDir.absolutePath)
    actual fun cachePath(context: PlatformContext, appName: String): File = File(context.cacheDir.absolutePath, "temp")
}