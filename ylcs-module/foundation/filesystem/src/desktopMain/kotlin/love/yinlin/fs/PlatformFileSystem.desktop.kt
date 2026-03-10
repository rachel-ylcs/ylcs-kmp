package love.yinlin.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.platform.Platform
import love.yinlin.platform.platform
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

actual object PlatformFileSystem {
    actual val PathSeparator: Char = File.separatorChar
    actual val LineSeparator: String = System.lineSeparator()

    actual fun appPath(context: PlatformContextDelegate, appName: String): Path {
        val workingDir = Path(System.getProperty("user.dir"))
        val homeDir = Path(System.getProperty("user.home"))
        return if (Files.isWritable(Paths.get(workingDir.toString()))) workingDir
        else {
            when (platform) {
                Platform.Windows -> System.getenv("APPDATA")?.let { Path(it, appName) } ?: workingDir
                Platform.Linux -> Path(System.getenv("XDG_DATA_HOME")?.let { Path(it) } ?: Path(homeDir, ".local", "share"), appName)
                Platform.MacOS -> Path(homeDir, "Library", "Application Support", appName)
                else -> workingDir
            }
        }
    }

    actual fun dataPath(context: PlatformContextDelegate, appName: String): Path = Path(appPath(context, appName), "data")

    actual fun cachePath(context: PlatformContextDelegate, appName: String): Path = Path(System.getProperty("java.io.tmpdir"), appName, "temp")

    @PublishedApi
    internal actual suspend fun exists(path: Path): Boolean = SystemFileSystem.exists(path)
    @PublishedApi
    internal actual suspend fun delete(path: Path, mustExist: Boolean) = SystemFileSystem.delete(path)
    @PublishedApi
    internal actual suspend fun createDirectories(path: Path, mustCreate: Boolean) = SystemFileSystem.createDirectories(path, mustCreate)
    @PublishedApi
    internal actual suspend fun atomicMove(source: Path, destination: Path) = SystemFileSystem.atomicMove(source, destination)
    @PublishedApi
    internal actual suspend fun source(path: Path): RawSource = SystemFileSystem.source(path)
    @PublishedApi
    internal actual suspend fun sink(path: Path, append: Boolean): RawSink = SystemFileSystem.sink(path, append)
    @PublishedApi
    internal actual suspend fun metadataOrNull(path: Path): FileMetadata? = SystemFileSystem.metadataOrNull(path)
    @PublishedApi
    internal actual suspend fun list(directory: Path): Collection<Path> = SystemFileSystem.list(directory)
}