@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.fs

import kotlinx.browser.window
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.await
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.io.WebFileSink
import love.yinlin.io.WebFileSource
import love.yinlin.platform.unsupportedPlatform
import kotlin.js.*

private fun webPlatformName(): String = js("(typeof navigator !== \"undefined\" && navigator.platform) || \"unknown\"")
private fun emptyOption(): JsAny = js("{}")
private fun recursiveOption(): JsAny = js("{ recursive: true }")
private fun createOption(): JsAny = js("{ create: true }")
private fun keepExistingDataOption(): JsAny = js("{ keepExistingData: true }")

actual object PlatformFileSystem {
    private val isWin32 by lazy { webPlatformName().startsWith("win", true) }

    actual val PathSeparator: Char = '/'
    actual val LineSeparator: String = if (isWin32) "\r\n" else "\n"

    private var rootHandle: FileSystemDirectoryHandle? = null

    private suspend fun root(): FileSystemDirectoryHandle {
        if (rootHandle == null) rootHandle = window.navigator.storage.getDirectory().await()
        return rootHandle!!
    }

    private suspend fun castParent(path: Path): Pair<FileSystemDirectoryHandle, String>? {
        val segments = path.toString().split(PathSeparator).filter { it.isNotEmpty() }
        val baseHandle = root()
        if (segments.isEmpty()) return baseHandle to ""

        var current = baseHandle
        for (i in 0 ..< segments.size - 1) current = catchingNull { current.getDirectoryHandle(segments[i]).await() } ?: return null
        return current to segments.last()
    }

    actual fun appPath(context: PlatformContextDelegate, appName: String): Path = Path("$PathSeparator$appName")

    actual fun dataPath(context: PlatformContextDelegate, appName: String): Path = Path("$PathSeparator$appName${PathSeparator}data")

    actual fun cachePath(context: PlatformContextDelegate, appName: String): Path = Path("$PathSeparator$appName${PathSeparator}cache${PathSeparator}temp")

    @PublishedApi
    internal actual suspend fun exists(path: Path): Boolean {
        val (parent, name) = castParent(path) ?: return false
        if (name.isEmpty()) return true

        return try {
            parent.getFileHandle(name).await()
            true
        } catch (_: Throwable) {
            catchingDefault(false) {
                parent.getDirectoryHandle(name).await()
                true
            }
        }
    }

    @PublishedApi
    internal actual suspend fun delete(path: Path, mustExist: Boolean) {
        val resolved = castParent(path)
        if (resolved == null) {
            require(!mustExist) { "Path not found: $path" }
            return
        }
        val (parent, name) = resolved
        require(name.isNotEmpty()) { "Cannot delete root directory" }

        catchingError {
            parent.removeEntry(name, recursiveOption()).await()
        }?.let {
            require(!mustExist) { "Failed to delete $path: ${it.message}" }
        }
    }

    @PublishedApi
    internal actual suspend fun createDirectories(path: Path, mustCreate: Boolean) {
        val segments = path.toString().split(PathSeparator).filter { it.isNotEmpty() }
        var current = root()

        for (i in segments.indices) {
            val segment = segments[i]
            val isLast = i == segments.size - 1

            if (mustCreate && isLast) {
                val alreadyExists = catchingDefault(false) {
                    current.getDirectoryHandle(segment).await()
                    true
                }
                require(!alreadyExists) { "Directory already exists: $path" }
            }

            current = current.getDirectoryHandle(segment, createOption()).await()
        }
    }

    @PublishedApi
    internal actual suspend fun atomicMove(source: Path, destination: Path) {
        unsupportedPlatform()
    }

    @PublishedApi
    internal actual suspend fun source(path: Path): RawSource {
        val (parent, name) = castParent(path) ?: error("File not found: $path")
        val fileHandle = parent.getFileHandle(name).await()
        val webFile = fileHandle.getFile().await()
        return WebFileSource(webFile)
    }

    @PublishedApi
    internal actual suspend fun sink(path: Path, append: Boolean): RawSink {
        val (parent, name) = castParent(path) ?: error("Parent dir not found: $path")
        val fileHandle = parent.getFileHandle(name, createOption()).await()
        val writeOptions = if (append) keepExistingDataOption() else emptyOption()
        val writableStream = fileHandle.createWritable(writeOptions).await()
        if (append) {
            val currentSize = fileHandle.getFile().await().size.toDouble()
            writableStream.seek(currentSize).await()
        }
        return WebFileSink(writableStream)
    }

    @PublishedApi
    internal actual suspend fun metadataOrNull(path: Path): FileMetadata? {
        val (parent, name) = castParent(path) ?: return null

        if (name.isEmpty()) return FileMetadata(isRegularFile = false, isDirectory = true)

        return try {
            val fileHandle = parent.getFileHandle(name).await()
            val file = fileHandle.getFile().await()
            FileMetadata(
                isRegularFile = true,
                isDirectory = false,
                size = file.size.toDouble().toLong()
            )
        } catch (_: Throwable) {
            catchingNull {
                parent.getDirectoryHandle(name).await()
                FileMetadata(isRegularFile = false, isDirectory = true)
            }
        }
    }

    @PublishedApi
    internal actual suspend fun list(directory: Path): Collection<Path> {
        val (parent, name) = castParent(directory) ?: return emptyList()

        val targetDir = if (name.isEmpty()) parent else catchingNull { parent.getDirectoryHandle(name).await() } ?: return emptyList()

        val keys = awaitEnumIterator<JsString>(targetDir).await()
        return keys.toList().map { key -> Path(directory.toString() + PathSeparator + key) }
    }
}