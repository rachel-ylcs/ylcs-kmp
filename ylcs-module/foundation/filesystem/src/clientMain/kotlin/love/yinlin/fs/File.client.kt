package love.yinlin.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.IOCoroutine

private class StorageFile(private val delegate: Path) : File() {
    override val name: String get() = delegate.name
    override val isAbsolute: Boolean get() = delegate.isAbsolute
    override val parent: File? get() = delegate.parent?.let(::StorageFile)

    override fun toString(): String = delegate.toString()
    override fun equals(other: Any?): Boolean = delegate == (other as? StorageFile)?.delegate
    override fun hashCode(): Int = delegate.hashCode()

    @IOCoroutine
    override suspend fun metadata(): FileMetadata? = Coroutines.io { SystemFileSystem.metadataOrNull(delegate) }
    @IOCoroutine
    override suspend fun delete() = Coroutines.io { SystemFileSystem.delete(delegate, mustExist = false) }
    @IOCoroutine
    override suspend fun mkdir() = Coroutines.io { SystemFileSystem.createDirectories(delegate, mustCreate = false) }
    @IOCoroutine
    override suspend fun move(dst: File) = Coroutines.io { SystemFileSystem.atomicMove(delegate, Path(dst.path)) }
    @IOCoroutine
    override suspend fun rawSource(): RawSource = Coroutines.io { SystemFileSystem.source(delegate) }
    @IOCoroutine
    override suspend fun rawSink(append: Boolean): RawSink = Coroutines.io { SystemFileSystem.sink(delegate, append) }
    @IOCoroutine
    override suspend fun list(): List<File> = Coroutines.io { SystemFileSystem.list(delegate).map(::StorageFile) }
}

actual fun buildFile(uri: String): File = StorageFile(Path(uri))
actual fun buildFile(parent: File, vararg parts: String): File = StorageFile(Path(parent.path, *parts))