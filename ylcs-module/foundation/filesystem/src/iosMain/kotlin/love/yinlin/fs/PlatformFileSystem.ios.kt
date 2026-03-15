package love.yinlin.fs

import love.yinlin.foundation.PlatformContextDelegate
import platform.Foundation.*

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"

    private fun searchPath(directory: NSSearchPathDirectory): File {
        val paths = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true)
        return File((paths.getOrNull(0) as? String) ?: "")
    }

    actual fun appPath(context: PlatformContextDelegate, appName: String): File = File(NSHomeDirectory())

    actual fun dataPath(context: PlatformContextDelegate, appName: String): File = searchPath(NSDocumentDirectory)

    actual fun cachePath(context: PlatformContextDelegate, appName: String): File = File(searchPath(NSCachesDirectory), "temp")
}