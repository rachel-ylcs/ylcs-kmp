package love.yinlin.fs

import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.platform.unsupportedPlatform

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"
    actual fun appPath(context: PlatformContextDelegate, appName: String): File = unsupportedPlatform()
    actual fun dataPath(context: PlatformContextDelegate, appName: String): File = unsupportedPlatform()
    actual fun cachePath(context: PlatformContextDelegate, appName: String): File = unsupportedPlatform()
}