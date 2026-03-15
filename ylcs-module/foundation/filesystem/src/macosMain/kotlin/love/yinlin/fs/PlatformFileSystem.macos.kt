package love.yinlin.fs

import kotlinx.cinterop.*
import love.yinlin.foundation.PlatformContextDelegate
import platform.Foundation.*
import platform.posix.getcwd

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"

    @OptIn(ExperimentalForeignApi::class)
    actual fun appPath(context: PlatformContextDelegate, appName: String): File = File(memScoped {
        val buffer = allocArray<ByteVar>(1024)
        getcwd(buffer, 1024UL)
        buffer.toKString()
    })

    actual fun dataPath(context: PlatformContextDelegate, appName: String): File = File(appPath(context, appName), "data")

    actual fun cachePath(context: PlatformContextDelegate, appName: String): File {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        return File((paths.getOrNull(0) as? String) ?: "", appName, "temp")
    }
}