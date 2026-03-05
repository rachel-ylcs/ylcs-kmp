package love.yinlin.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path

expect object PlatformFileSystem {
    /**
     * 初始化文件系统
     */
    suspend fun init()

    /**
     * 路径分隔符
     */
    val PathSeparator: Char

    /**
     * 换行符
     */
    val LineSeparator: String

    @PublishedApi
    internal fun exists(path: Path): Boolean

    @PublishedApi
    internal fun delete(path: Path, mustExist: Boolean = true)

    @PublishedApi
    internal fun createDirectories(path: Path, mustCreate: Boolean = false)

    @PublishedApi
    internal fun atomicMove(source: Path, destination: Path)

    @PublishedApi
    internal fun source(path: Path): RawSource

    @PublishedApi
    internal fun sink(path: Path, append: Boolean = false): RawSink

    @PublishedApi
    internal fun metadataOrNull(path: Path): FileMetadata?

    @PublishedApi
    internal fun resolve(path: Path): Path

    @PublishedApi
    internal fun list(directory: Path): Collection<Path>
}