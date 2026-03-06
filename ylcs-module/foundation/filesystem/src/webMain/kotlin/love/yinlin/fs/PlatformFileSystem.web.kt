@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.fs

import kotlinx.browser.window
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.await
import love.yinlin.extension.catchingNull
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

private fun webPlatformName(): String = js("(typeof navigator !== \"undefined\" && navigator.platform) || \"unknown\"")

actual object PlatformFileSystem {
    private val isWin32 by lazy { webPlatformName().startsWith("win", true) }

    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = if (isWin32) "\r\n" else "\n"

    private var rootHandle: FileSystemDirectoryHandle? = null

    private suspend fun root(): FileSystemDirectoryHandle? {
        if (rootHandle == null) rootHandle = catchingNull { window.navigator.storage.getDirectory().await() }
        return rootHandle
    }

    @PublishedApi
    internal actual suspend fun exists(path: Path): Boolean = false

    @PublishedApi
    internal actual suspend fun delete(path: Path, mustExist: Boolean) { }

    @PublishedApi
    internal actual suspend fun createDirectories(path: Path, mustCreate: Boolean) { }

    @PublishedApi
    internal actual suspend fun atomicMove(source: Path, destination: Path) { }

    @PublishedApi
    internal actual suspend fun source(path: Path): RawSource = TODO()

    @PublishedApi
    internal actual suspend fun sink(path: Path, append: Boolean): RawSink = TODO()

    @PublishedApi
    internal actual suspend fun metadataOrNull(path: Path): FileMetadata? = null

    @PublishedApi
    internal actual suspend fun list(directory: Path): Collection<Path> = emptyList()
}