package love.yinlin.fs

import java.io.File as JFile
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.files.FileMetadata
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.IOCoroutine
import love.yinlin.extension.catching
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private class JvmFile(private val delegate: JFile) : File() {
    override val name: String get() = delegate.name
    override val isAbsolute: Boolean get() = delegate.isAbsolute
    override val parent: File? get() = delegate.parentFile?.let(::JvmFile)

    override fun toString(): String = delegate.toString()
    override fun equals(other: Any?): Boolean = delegate == (other as? JvmFile)?.delegate
    override fun hashCode(): Int = delegate.hashCode()

    @IOCoroutine
    override suspend fun metadata(): FileMetadata? = Coroutines.io { metadataSync() }

    override fun metadataSync(): FileMetadata? {
        if (!delegate.exists()) return null
        val isFile = delegate.isFile
        return FileMetadata(isFile, delegate.isDirectory, if (isFile) delegate.length() else -1L)
    }

    @IOCoroutine
    override suspend fun delete() = Coroutines.io { deleteSync() }

    override fun deleteSync() {
        if (delegate.exists()) delegate.delete()
    }

    @IOCoroutine
    override suspend fun mkdir() = Coroutines.io { mkdirSync() }

    override fun mkdirSync() { delegate.mkdirs() }

    @IOCoroutine
    override suspend fun move(dst: File) = Coroutines.io { moveSync(dst) }

    override fun moveSync(dst: File) {
        catching {
            Files.move(delegate.toPath(), JFile(dst.path).toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    @IOCoroutine
    override suspend fun rawSource(): RawSource = Coroutines.io { rawSourceSync() }

    override fun rawSourceSync(): RawSource = FileInputStream(delegate).asSource()

    @IOCoroutine
    override suspend fun rawSink(append: Boolean): RawSink = Coroutines.io { rawSinkSync(append) }

    override fun rawSinkSync(append: Boolean): RawSink = FileOutputStream(delegate, append).asSink()

    @IOCoroutine
    override suspend fun list(): List<File> = Coroutines.io { listSync() }

    override fun listSync(): List<File> {
        val fileList = if (delegate.isDirectory) delegate.list() else null
        return fileList?.map { File(this, it) } ?: emptyList()
    }
}

actual fun buildFile(uri: String): File = JvmFile(JFile(uri))
actual fun buildFile(parent: File, vararg parts: String): File {
    var jFile = JFile(parent.path)
    for (part in parts) jFile = jFile.resolve(part)
    return JvmFile(jFile)
}