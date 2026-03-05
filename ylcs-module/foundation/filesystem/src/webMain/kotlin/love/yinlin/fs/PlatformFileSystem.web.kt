package love.yinlin.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

@OptIn(ExperimentalWasmJsInterop::class)
private fun webPlatformName(): String = js("(typeof navigator !== \"undefined\" && navigator.platform) || \"unknown\"")

actual object PlatformFileSystem {
    private val isWin32 by lazy { webPlatformName().startsWith("win", true) }

    actual suspend fun init() { }
    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = if (isWin32) "\r\n" else "\n"

    @PublishedApi
    internal actual fun exists(path: Path): Boolean = false
    @PublishedApi
    internal actual fun delete(path: Path, mustExist: Boolean) {}
    @PublishedApi
    internal actual fun createDirectories(path: Path, mustCreate: Boolean) {}
    @PublishedApi
    internal actual fun atomicMove(source: Path, destination: Path) {}
    @PublishedApi
    internal actual fun source(path: Path): RawSource = TODO()
    @PublishedApi
    internal actual fun sink(path: Path, append: Boolean): RawSink = TODO()
    @PublishedApi
    internal actual fun metadataOrNull(path: Path): FileMetadata? = null
    @PublishedApi
    internal actual fun resolve(path: Path): Path = path
    @PublishedApi
    internal actual fun list(directory: Path): Collection<Path> = emptyList()
}