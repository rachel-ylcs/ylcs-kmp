package love.yinlin.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"

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