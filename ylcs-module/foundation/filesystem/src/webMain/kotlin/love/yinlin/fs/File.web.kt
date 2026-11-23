@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.fs

import kotlinx.coroutines.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.asArray
import love.yinlin.extension.catchingError
import love.yinlin.io.ArrayBufferSource
import love.yinlin.io.WebFileSink
import love.yinlin.platform.unsupportedPlatform
import org.khronos.webgl.ArrayBuffer
import kotlin.js.*

private fun createOption(): JsAny = js("({ create: true })")
private fun recursiveOption(): JsAny = js("({ recursive: true })")
private fun writeOption(append: Boolean): JsAny = js("({ keepExistingData: append, mode: 'exclusive' })")
private fun fileArrayBuffer(file: org.w3c.files.File): Promise<ArrayBuffer> = js("file.arrayBuffer()")

private suspend fun FileSystemDirectoryHandle.directory(name: String): FileSystemDirectoryHandle? = lookupEntry(getDirectoryHandle(name)).await()

private suspend fun FileSystemDirectoryHandle.file(name: String): FileSystemFileHandle? = lookupEntry(getFileHandle(name)).await()

private suspend fun FileSystemDirectoryHandle.names(): List<String> = awaitEnumIterator<JsString>(keys()).await().asArray.asList()

private suspend fun FileSystemFileHandle.openWritable(append: Boolean = false): FileSystemWritableFileStream {
    val stream = Coroutines.wait {
        createWritable(writeOption(append)).await()
    }

    try {
        currentCoroutineContext().ensureActive()
        if (append) stream.seek(getFile().await().size.toDouble()).await()
        return stream
    }
    catch (error: Throwable) {
        stream.abortAfter(error)
        throw error
    }
}

private suspend fun FileSystemWritableFileStream.abortAfter(error: Throwable) = Coroutines.wait {
    catchingError {
        abort().await()
    }?.let { error.addSuppressed(it) }
}

private class WebFile private constructor(private val uri: String, private val segments: List<String>) : File() {
    companion object {
        fun from(path: String): WebFile {
            val separators = path.replace('\\', '/')
            val absolute = separators.startsWith('/')
            val segments = mutableListOf<String>()
            for (part in separators.split('/')) {
                when (part) {
                    "", "." -> {}
                    ".." -> {
                        require(segments.isNotEmpty()) { "Path escapes the OPFS root: $path" }
                        segments.removeAt(segments.lastIndex)
                    }
                    else -> segments.add(part)
                }
            }
            val prefix = if (absolute) "/" else ""
            return WebFile(prefix + segments.joinToString("/"), segments)
        }
    }

    private suspend fun resolve(parts: List<String>): FileSystemDirectoryHandle? {
        var directory = openOpfs().await()
        for (part in parts) directory = directory.directory(part) ?: return null
        return directory
    }

    private suspend fun parentHandle(): FileSystemDirectoryHandle? = resolve(segments.dropLast(1))

    override val name: String get() = segments.lastOrNull() ?: ""
    override val isAbsolute: Boolean get() = uri.startsWith('/')
    override val parent: File? get() = if (segments.isEmpty() || '/' !in uri) null else from(uri.substringBeforeLast('/').ifEmpty { "/" })

    override fun toString(): String = uri
    override fun equals(other: Any?): Boolean = uri == (other as? WebFile)?.uri
    override fun hashCode(): Int = uri.hashCode()

    override suspend fun metadata(): FileMetadata? = Coroutines.io {
        val parent = parentHandle() ?: return@io null
        if (segments.isEmpty()) return@io FileMetadata(isRegularFile = false, isDirectory = true)

        val handle = parent.file(name)
        if (handle != null) {
            val snapshot = lookupEntry(handle.getFile()).await() ?: return@io null
            FileMetadata(isRegularFile = true, isDirectory = false, size = snapshot.size.toDouble().toLong())
        }
        else if (parent.directory(name) != null) FileMetadata(isRegularFile = false, isDirectory = true)
        else null
    }

    override fun metadataSync(): FileMetadata? = unsupportedPlatform()

    override suspend fun delete(): Unit = Coroutines.io {
        require(segments.isNotEmpty()) { "Cannot delete the OPFS root" }
        val parent = parentHandle() ?: return@io
        ignoreMissingEntry(parent.removeEntry(name)).await()
    }

    override fun deleteSync(): Unit = unsupportedPlatform()

    override suspend fun mkdir(): Unit = Coroutines.io {
        var directory = openOpfs().await()
        for (part in segments) directory = directory.getDirectoryHandle(part, createOption()).await()
    }

    override fun mkdirSync(): Unit = unsupportedPlatform()

    override suspend fun move(dst: File): Unit = Coroutines.io {
        val target = from(dst.path)
        require(segments.isNotEmpty() && target.segments.isNotEmpty()) { "Cannot move the OPFS root" }
        val sourceParent = parentHandle() ?: throw IOException("Source not found: $uri")
        val sourceFile = sourceParent.file(name)
        val sourceDirectory = if (sourceFile == null) sourceParent.directory(name) else null
        if (sourceFile == null && sourceDirectory == null) throw IOException("Source not found: $uri")
        if (segments == target.segments) return@io
        require(sourceDirectory == null || target.segments.take(segments.size) != segments) { "Cannot move a directory into itself" }

        val targetParent = target.parentHandle() ?: throw IOException("Destination parent not found: ${target.path}")
        val targetName = target.name
        if (sourceFile != null) {
            if (moveOpfsFile(sourceFile, targetParent, targetName).await().toBoolean()) return@io
            copyFile(sourceFile, targetParent, targetName)
        }
        else {
            check(targetParent.file(targetName) == null && targetParent.directory(targetName) == null) { "Destination already exists: ${target.path}" }
            val targetDirectory = Coroutines.wait { targetParent.getDirectoryHandle(targetName, createOption()).await() }
            val copiedEntries = mutableListOf<Pair<FileSystemDirectoryHandle, String>>()
            try {
                copyDirectory(checkNotNull(sourceDirectory), targetDirectory, copiedEntries)
            }
            catch (error: Throwable) {
                cleanupCopy(targetParent, targetName, error)
                throw error
            }
            // 按从叶到根的顺序删除已复制条目，保留移动期间新建的其他条目
            for ([directory, entry] in copiedEntries) ignoreMissingEntry(directory.removeEntry(entry)).await()
        }
        // 保留已经提交的目标，即使删除源失败
        sourceParent.removeEntry(name).await()
    }

    private suspend fun copyDirectory(
        source: FileSystemDirectoryHandle,
        target: FileSystemDirectoryHandle,
        copiedEntries: MutableList<Pair<FileSystemDirectoryHandle, String>>
    ) {
        for (name in source.names()) {
            val file = source.file(name)
            if (file != null) copyFile(file, target, name)
            else {
                val directory = source.getDirectoryHandle(name).await()
                val child = target.getDirectoryHandle(name, createOption()).await()
                copyDirectory(directory, child, copiedEntries)
            }
            copiedEntries.add(source to name)
        }
    }

    private suspend fun copyFile(source: FileSystemFileHandle, parent: FileSystemDirectoryHandle, name: String) {
        val snapshot = source.getFile().await()
        val existing = parent.file(name)
        val target = existing ?: Coroutines.wait { parent.getFileHandle(name, createOption()).await() }
        try {
            currentCoroutineContext().ensureActive()
            val stream = target.openWritable()
            try {
                stream.write(snapshot).await()
                currentCoroutineContext().ensureActive()
                Coroutines.wait { stream.close().await() }
            }
            catch (error: Throwable) {
                stream.abortAfter(error)
                throw error
            }
        }
        catch (error: Throwable) {
            if (existing == null) cleanupCopy(parent, name, error)
            throw error
        }
    }

    private suspend fun cleanupCopy(parent: FileSystemDirectoryHandle, name: String, error: Throwable) = Coroutines.wait {
        catchingError {
            ignoreMissingEntry(parent.removeEntry(name, recursiveOption())).await()
        }?.let { error.addSuppressed(it) }
    }

    override fun moveSync(dst: File): Unit = unsupportedPlatform()

    override suspend fun rawSource(): RawSource = Coroutines.io {
        val file = if (segments.isEmpty()) null else parentHandle()?.file(name)
        if (file == null) throw IOException("Not a file: $uri")
        ArrayBufferSource(fileArrayBuffer(file.getFile().await()).await())
    }

    override fun rawSourceSync(): RawSource = unsupportedPlatform()

    override suspend fun rawSink(append: Boolean): RawSink {
        require(segments.isNotEmpty()) { "Cannot write to the OPFS root" }
        val parent = parentHandle() ?: throw IOException("Parent directory not found: $uri")
        val file = parent.getFileHandle(name, createOption()).await()
        return WebFileSink(file.openWritable(append))
    }

    override fun rawSinkSync(append: Boolean): RawSink = unsupportedPlatform()

    override suspend fun list(): List<File> = Coroutines.io {
        val directory = resolve(segments) ?: return@io []
        directory.names().map { File(this@WebFile, it) }
    }

    override fun listSync(): List<File> = unsupportedPlatform()
}

actual fun buildFile(uri: String): File = WebFile.from(uri)
actual fun buildFile(parent: File, vararg parts: String): File = WebFile.from(([parent.path] + parts).filter { it.isNotEmpty() }.joinToString("/"))
