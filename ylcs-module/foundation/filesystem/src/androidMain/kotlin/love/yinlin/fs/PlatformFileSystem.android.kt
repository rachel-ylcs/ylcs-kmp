package love.yinlin.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.foundation.PlatformContextDelegate
import java.io.File

actual object PlatformFileSystem {
    actual val PathSeparator: Char = File.separatorChar
    actual val LineSeparator: String = System.lineSeparator()

    actual fun appPath(context: PlatformContextDelegate, appName: String): Path = Path(context.dataDir.absolutePath)

    actual fun dataPath(context: PlatformContextDelegate, appName: String): Path = run {

        Path(context.filesDir.absolutePath)
    }

    actual fun cachePath(context: PlatformContextDelegate, appName: String): Path = Path(context.cacheDir.absolutePath, "temp")

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