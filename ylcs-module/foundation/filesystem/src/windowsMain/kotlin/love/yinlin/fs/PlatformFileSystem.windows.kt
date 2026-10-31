package love.yinlin.fs

import kotlinx.cinterop.*
import love.yinlin.foundation.PlatformContext
import platform.windows.GetCurrentDirectoryW
import platform.windows.SetCurrentDirectoryW

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '\\'
    actual val LineSeparator: String = "\r\n"

    actual fun appPath(context: PlatformContext, appName: String): File = checkNotNull(getCurrentDirectory()) { "Cannot read the current working directory" }

    actual fun dataPath(context: PlatformContext, appName: String): File = File(appPath(context, appName), "data")

    actual fun cachePath(context: PlatformContext, appName: String): File = File(StandardPath.Temp.path, appName, "temp")

    @OptIn(ExperimentalForeignApi::class)
    actual fun getCurrentDirectory(): File? {
        var capacity = GetCurrentDirectoryW(0u, null)
        while (capacity != 0u && capacity <= Int.MAX_VALUE.toUInt()) {
            capacity = memScoped {
                val buffer = allocArray<UShortVar>(capacity.toInt())
                val length = GetCurrentDirectoryW(capacity, buffer)
                if (length == 0u) return null
                if (length < capacity) return File(buffer.toKString())
                length
            }
        }
        return null
    }

    actual fun setCurrentDirectory(path: File): Boolean = SetCurrentDirectoryW(path.path) != 0
}