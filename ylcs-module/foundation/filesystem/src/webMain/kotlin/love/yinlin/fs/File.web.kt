@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.fs

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.IOCoroutine
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.io.WebFileSink
import love.yinlin.io.WebFileSource
import love.yinlin.platform.unsupportedPlatform
import kotlin.js.*

private fun emptyOption(): JsAny = js("{}")
private fun recursiveOption(): JsAny = js("{ recursive: true }")
private fun createOption(): JsAny = js("{ create: true }")
private fun keepExistingDataOption(): JsAny = js("{ keepExistingData: true }")

@Suppress("unused")
private class WebFile private constructor(private val uri: String, private val unused: Boolean) : File() {
    companion object {
        private fun normalize(rawPath: String): String {
            var processed = rawPath.replace('\\', '/')
            processed = processed.replace(Regex("/{2,}"), "/")
            if (processed.length > 1 && processed.endsWith("/")) processed = processed.substring(0, processed.length - 1)
            return processed
        }

        private var rootHandle: FileSystemDirectoryHandle? = null

        private suspend fun root(): FileSystemDirectoryHandle? {
            if (rootHandle == null) rootHandle = window.navigator.storage.getDirectory().await()
            return rootHandle
        }

        private suspend fun WebFile.castParent(): Pair<FileSystemDirectoryHandle, String>? {
            val segments = uri.split('/').filter { it.isNotEmpty() }
            val baseHandle = root() ?: return null
            if (segments.isEmpty()) return baseHandle to ""

            var current = baseHandle
            for (i in 0 ..< segments.size - 1) current = catchingNull { current.getDirectoryHandle(segments[i]).await() } ?: return null
            return current to segments.last()
        }
    }

    constructor(uri: String) : this(normalize(uri), true)
    constructor(uri: String, vararg parts: String) : this(normalize((listOf(uri) + parts).joinToString("/")), true)

    override val name: String get() = if (uri == "/") "" else uri.substringAfterLast('/')
    override val isAbsolute: Boolean get() = uri.startsWith('/')
    override val parent: File? get() = if (uri == "") null else when (val lastSlashIndex = uri.lastIndexOf('/')) {
        -1 -> null
        0 -> WebFile("/", true)
        else -> WebFile(uri.substring(0, lastSlashIndex), true)
    }

    override fun toString(): String = uri
    override fun equals(other: Any?): Boolean = uri == (other as? WebFile)?.uri
    override fun hashCode(): Int = uri.hashCode()

    @IOCoroutine
    override suspend fun metadata(): FileMetadata? = Coroutines.io {
        val (parent, name) = castParent() ?: return@io null

        if (name.isEmpty()) FileMetadata(isRegularFile = false, isDirectory = true)
        else try {
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

    @IOCoroutine
    override suspend fun delete() = Coroutines.io {
        val resolved = castParent() ?: return@io
        val (parent, name) = resolved
        require(name.isNotEmpty()) { "Cannot delete root directory" }
        catchingError { parent.removeEntry(name, recursiveOption()).await() }
    }

    @IOCoroutine
    override suspend fun mkdir() = Coroutines.io {
        val segments = uri.split('/').filter { it.isNotEmpty() }
        var current = root() ?: return@io

        for (i in segments.indices) current = current.getDirectoryHandle(segments[i], createOption()).await()
    }

    @IOCoroutine
    override suspend fun move(dst: File) = unsupportedPlatform()

    @IOCoroutine
    override suspend fun rawSource(): RawSource = Coroutines.io {
        val (parent, name) = castParent() ?: error("File not found: $uri")
        val fileHandle = parent.getFileHandle(name).await()
        WebFileSource(fileHandle.getFile().await())
    }

    @IOCoroutine
    override suspend fun rawSink(append: Boolean): RawSink = Coroutines.io {
        val (parent, name) = castParent() ?: error("Parent dir not found: $uri")
        val fileHandle = parent.getFileHandle(name, createOption()).await()
        val writeOptions = if (append) keepExistingDataOption() else emptyOption()
        val writableStream = fileHandle.createWritable(writeOptions).await()
        if (append) {
            val currentSize = fileHandle.getFile().await().size.toDouble()
            writableStream.seek(currentSize).await()
        }
        WebFileSink(writableStream)
    }

    @IOCoroutine
    override suspend fun list(): List<File> = Coroutines.io {
        val (parent, name) = castParent() ?: return@io emptyList()

        val targetDir = if (name.isEmpty()) parent else catchingNull { parent.getDirectoryHandle(name).await() } ?: return@io emptyList()

        val keys = awaitEnumIterator<JsString>(targetDir).await()
        keys.toList().map { key -> File("$uri/$key") }
    }
}

actual fun buildFile(uri: String): File = WebFile(uri)
actual fun buildFile(parent: File, vararg parts: String): File = WebFile(parent.path, *parts)