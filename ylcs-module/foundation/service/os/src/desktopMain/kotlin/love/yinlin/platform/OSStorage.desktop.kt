package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.extension.deleteRecursively
import love.yinlin.extension.mkdir
import love.yinlin.extension.size
import love.yinlin.extension.toNioPath
import java.nio.file.Files

actual fun buildOSStorage(context: Context, appName: String): OSStorage = object : OSStorage() {
    override val appPath: Path = run {
        val workingDir = Path(System.getProperty("user.dir"))
        val homeDir = Path(System.getProperty("user.home"))
        if (Files.isWritable(workingDir.toNioPath())) workingDir
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