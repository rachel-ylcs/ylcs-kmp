package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.extension.toNioPath
import love.yinlin.service.PlatformContext
import java.nio.file.Files

actual fun buildOSStorage(context: PlatformContext, appName: String): OSStorage = object : OSStorage() {
    override val dataPath: Path = run {
        val workingDir = Path(System.getProperty("user.dir"))
        val homeDir = Path(System.getProperty("user.home"))
        val appPath = if (Files.isWritable(workingDir.toNioPath())) workingDir else {
            when (platform) {
                Platform.Windows -> System.getenv("APPDATA")?.let { Path(it, appName) } ?: workingDir
                Platform.Linux -> Path(System.getenv("XDG_DATA_HOME")?.let { Path(it) } ?: Path(homeDir, ".local", "share"), appName)
                Platform.MacOS -> Path(homeDir, "Library", "Application Support", appName)
                else -> workingDir
            }
        }
        Path(appPath, "data")
    }

    override val cachePath: Path = Path(System.getProperty("java.io.tmpdir"), appName)

    override val cacheSize: Long get() {
        // TODO:
        return 0L
    }

    override fun clearCache() {

    }
}