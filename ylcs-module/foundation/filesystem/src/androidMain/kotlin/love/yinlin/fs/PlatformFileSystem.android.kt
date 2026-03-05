package love.yinlin.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.File

actual object PlatformFileSystem {
    actual suspend fun init() { }
    actual val PathSeparator: Char = File.separatorChar
    actual val LineSeparator: String = System.lineSeparator()

    @PublishedApi
    internal actual fun exists(path: Path): Boolean = SystemFileSystem.exists(path)
    @PublishedApi
    internal actual fun delete(path: Path, mustExist: Boolean) = SystemFileSystem.delete(path)
    @PublishedApi
    internal actual fun createDirectories(path: Path, mustCreate: Boolean) = SystemFileSystem.createDirectories(path, mustCreate)
    @PublishedApi
    internal actual fun atomicMove(source: Path, destination: Path) = SystemFileSystem.atomicMove(source, destination)
    @PublishedApi
    internal actual fun source(path: Path): RawSource = SystemFileSystem.source(path)
    @PublishedApi
    internal actual fun sink(path: Path, append: Boolean): RawSink = SystemFileSystem.sink(path, append)
    @PublishedApi
    internal actual fun metadataOrNull(path: Path): FileMetadata? = SystemFileSystem.metadataOrNull(path)
    @PublishedApi
    internal actual fun resolve(path: Path): Path = SystemFileSystem.resolve(path)
    @PublishedApi
    internal actual fun list(directory: Path): Collection<Path> = SystemFileSystem.list(directory)
}