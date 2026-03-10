package love.yinlin.fs

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.foundation.PlatformContextDelegate
import platform.posix.getcwd
import platform.posix.getenv

actual object PlatformFileSystem {
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = "\n"

    @OptIn(ExperimentalForeignApi::class)
    actual fun appPath(context: PlatformContextDelegate, appName: String): Path = Path(memScoped {
        val buffer = allocArray<ByteVar>(1024)
        getcwd(buffer, 1024UL)
        buffer.toKString()
    })

    actual fun dataPath(context: PlatformContextDelegate, appName: String): Path = Path(appPath(context, appName), "data")

    @OptIn(ExperimentalForeignApi::class)
    actual fun cachePath(context: PlatformContextDelegate, appName: String): Path {
        val xdgCache = getenv("XDG_CACHE_HOME")?.toKString()
        if (!xdgCache.isNullOrBlank()) return Path(xdgCache, appName, "temp")
        val home = getenv("HOME")?.toKString()
        if (!home.isNullOrBlank()) return Path(home, ".cache", appName, "temp")
        return Path("/tmp", appName, "temp")
    }

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