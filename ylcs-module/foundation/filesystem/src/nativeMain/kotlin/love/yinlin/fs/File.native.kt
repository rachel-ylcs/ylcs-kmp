package love.yinlin.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.IOCoroutine
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

/**
 * kotlinx-io 库在 native 上没有考虑 wchar 的路径转换
 * 待上游修复，所以采用手动实现
 * 参看 https://github.com/Kotlin/kotlinx-io/issues/522
 */
private class NativeFile(private val delegate: Path) : File() {
    override val name: String get() {
        val path = delegate.toString()
        if (path.isEmpty()) return ""
        val lastSlash = if (platform == Platform.WindowsNative) path.lastIndexOfAny(charArrayOf('\\', '/')) else path.lastIndexOf('/')
        return if (lastSlash != -1) path.substring(lastSlash + 1) else path
    }

    override val isAbsolute: Boolean get() {
        val path = delegate.toString()
        return if (platform == Platform.WindowsNative) {
            path.length >= 3 && path[0].isLetter() && path[1] == ':' && (path[2] == '\\' || path[2] == '/')
        } else path.startsWith("/")
    }

    override val parent: File? get() {
        val path = delegate.toString()
        if (path.isEmpty()) return null

        val lastSlash = if (platform == Platform.WindowsNative) path.lastIndexOfAny(charArrayOf('\\', '/')) else path.lastIndexOf('/')
        if (lastSlash != -1) {
            val parentPath = if (platform == Platform.WindowsNative && lastSlash == 2 && path[1] == ':') path.substring(0, 3)
            else if (lastSlash == 0) path.substring(0, 1)
            else path.substring(0, lastSlash)
            return File(parentPath)
        }
        return null
    }

    override fun toString(): String = delegate.toString()
    override fun equals(other: Any?): Boolean = delegate == (other as? NativeFile)?.delegate
    override fun hashCode(): Int = delegate.hashCode()

    @IOCoroutine
    override suspend fun metadata(): FileMetadata? = Coroutines.io { SystemFileSystem.metadataOrNull(delegate) }

    override fun metadataSync(): FileMetadata? = SystemFileSystem.metadataOrNull(delegate)

    @IOCoroutine
    override suspend fun delete() = Coroutines.io { SystemFileSystem.delete(delegate, mustExist = false) }

    override fun deleteSync() = SystemFileSystem.delete(delegate, mustExist = false)

    @IOCoroutine
    override suspend fun mkdir() = Coroutines.io { SystemFileSystem.createDirectories(delegate, mustCreate = false) }

    override fun mkdirSync() = SystemFileSystem.createDirectories(delegate, mustCreate = false)

    @IOCoroutine
    override suspend fun move(dst: File) = Coroutines.io { SystemFileSystem.atomicMove(delegate, Path(dst.path)) }

    override fun moveSync(dst: File) = SystemFileSystem.atomicMove(delegate, Path(dst.path))

    @IOCoroutine
    override suspend fun rawSource(): RawSource = Coroutines.io { SystemFileSystem.source(delegate) }

    override fun rawSourceSync(): RawSource = SystemFileSystem.source(delegate)

    @IOCoroutine
    override suspend fun rawSink(append: Boolean): RawSink = Coroutines.io { SystemFileSystem.sink(delegate, append) }

    override fun rawSinkSync(append: Boolean): RawSink = SystemFileSystem.sink(delegate, append)

    @IOCoroutine
    override suspend fun list(): List<File> = Coroutines.io { SystemFileSystem.list(delegate).map(::NativeFile) }

    override fun listSync(): List<File> = SystemFileSystem.list(delegate).map(::NativeFile)
}

actual fun buildFile(uri: String): File = NativeFile(Path(uri))
actual fun buildFile(parent: File, vararg parts: String): File = NativeFile(Path(parent.path, *parts))