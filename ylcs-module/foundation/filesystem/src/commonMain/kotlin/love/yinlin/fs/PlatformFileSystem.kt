package love.yinlin.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path

expect object PlatformFileSystem {
    /**
     * 路径分隔符
     */
    val PathSeparator: Char

    /**
     * 换行符
     */
    val LineSeparator: String

    @PublishedApi
    internal suspend fun exists(path: Path): Boolean

    @PublishedApi
    internal suspend fun delete(path: Path, mustExist: Boolean = true)

    @PublishedApi
    internal suspend fun createDirectories(path: Path, mustCreate: Boolean = false)

    @PublishedApi
    internal suspend fun atomicMove(source: Path, destination: Path)

    @PublishedApi
    internal suspend fun source(path: Path): RawSource

    @PublishedApi
    internal suspend fun sink(path: Path, append: Boolean = false): RawSink

    @PublishedApi
    internal suspend fun metadataOrNull(path: Path): FileMetadata?

    @PublishedApi
    internal suspend fun list(directory: Path): Collection<Path>
}