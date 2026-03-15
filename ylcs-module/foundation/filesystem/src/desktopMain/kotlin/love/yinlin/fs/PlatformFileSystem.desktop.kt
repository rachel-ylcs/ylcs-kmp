package love.yinlin.fs

import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.platform.Platform
import love.yinlin.platform.platform
import java.nio.file.Files
import java.nio.file.Paths

actual object PlatformFileSystem {
    actual val PathSeparator: Char = java.io.File.separatorChar
    actual val LineSeparator: String = System.lineSeparator()

    actual fun appPath(context: PlatformContextDelegate, appName: String): File {
        val workingDir = File(System.getProperty("user.dir"))
        val homeDir = File(System.getProperty("user.home"))
        return if (Files.isWritable(Paths.get(workingDir.toString()))) workingDir else when (platform) {
            Platform.Windows -> System.getenv("APPDATA")?.let { File(it, appName) } ?: workingDir
            Platform.Linux -> File(System.getenv("XDG_DATA_HOME")?.let { File(it) } ?: File(homeDir, ".local", "share"), appName)
            Platform.MacOS -> File(homeDir, "Library", "Application Support", appName)
            else -> workingDir
        }
    }

    actual fun dataPath(context: PlatformContextDelegate, appName: String): File = File(appPath(context, appName), "data")

    actual fun cachePath(context: PlatformContextDelegate, appName: String): File = File(System.getProperty("java.io.tmpdir"), appName, "temp")
}