package love.yinlin.fs

import love.yinlin.foundation.PlatformContext
import love.yinlin.platform.unsupportedPlatform

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"
    actual fun appPath(context: PlatformContext, appName: String): File = unsupportedPlatform()
    actual fun dataPath(context: PlatformContext, appName: String): File = unsupportedPlatform()
    actual fun cachePath(context: PlatformContext, appName: String): File = unsupportedPlatform()
}