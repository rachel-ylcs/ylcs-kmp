package love.yinlin.fs

import kotlinx.cinterop.*
import love.yinlin.foundation.PlatformContext
import platform.windows.*

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '\\'
    actual val LineSeparator: String = "\r\n"

    @OptIn(ExperimentalForeignApi::class)
    actual fun appPath(context: PlatformContext, appName: String): File {
        val workingDir = getCurrentDirectory()
        if (workingDir != null) {
            val handle = CreateFileW(
                lpFileName = workingDir.path,
                dwDesiredAccess = GENERIC_WRITE.convert<UInt>(),
                dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE or FILE_SHARE_DELETE).convert<UInt>(),
                lpSecurityAttributes = null,
                dwCreationDisposition = OPEN_EXISTING.convert<UInt>(),
                dwFlagsAndAttributes = FILE_FLAG_BACKUP_SEMANTICS.convert<UInt>(),
                hTemplateFile = null
            )
            if (handle != INVALID_HANDLE_VALUE) {
                CloseHandle(handle)
                return workingDir
            }
        }
        val appData = StandardPath.AppData.path
        return if (appData.isNotBlank()) File(appData, appName) else checkNotNull(workingDir) { "Cannot read the current working directory" }
    }

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