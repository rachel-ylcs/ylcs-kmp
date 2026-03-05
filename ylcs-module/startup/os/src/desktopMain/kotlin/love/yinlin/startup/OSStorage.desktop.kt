package love.yinlin.startup

import kotlinx.io.files.Path
import love.yinlin.foundation.Context
import love.yinlin.fs.deleteRecursively
import love.yinlin.fs.mkdir
import love.yinlin.fs.size
import love.yinlin.platform.Platform
import love.yinlin.platform.platform
import java.nio.file.Files
import java.nio.file.Paths

actual fun buildOSStorage(context: Context, appName: String): OSStorage = object : OSStorage() {
    override val appPath: Path = run {
        val workingDir = Path(System.getProperty("user.dir"))
        val homeDir = Path(System.getProperty("user.home"))
        if (Files.isWritable(Paths.get(workingDir.toString()))) workingDir
        else {
            when (platform) {
                Platform.Windows -> System.getenv("APPDATA")?.let { Path(it, appName) } ?: workingDir
                Platform.Linux -> Path(System.getenv("XDG_DATA_HOME")?.let { Path(it) } ?: Path(homeDir, ".local", "share"), appName)
                Platform.MacOS -> Path(homeDir, "Library", "Application Support", appName)
                else -> workingDir
            }
        }
    }

    override val dataPath: Path = Path(appPath, "data")

    override val cachePath: Path = Path(System.getProperty("java.io.tmpdir"), appName, "temp")

    override suspend fun calcCacheSize(): Long = cachePath.parent?.size ?: 0L

    override suspend fun clearCache() {
        cachePath.deleteRecursively()
        cachePath.mkdir()
    }
}