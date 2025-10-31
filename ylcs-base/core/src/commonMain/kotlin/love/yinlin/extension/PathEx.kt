package love.yinlin.extension

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString

val Path.extension: String get() = this.name.substringAfterLast('.')

val Path.nameWithoutExtension: String get() = this.name.substringBeforeLast('.')

val Path.exists: Boolean get() = SystemFileSystem.metadataOrNull(this) != null

val Path.size: Long get() = SystemFileSystem.metadataOrNull(this)?.size ?: 0L

val Path.isFile: Boolean get() = SystemFileSystem.metadataOrNull(this)?.isRegularFile ?: false

val Path.isDirectory: Boolean get() = SystemFileSystem.metadataOrNull(this)?.isDirectory ?: false

fun Path.list(): List<Path> = catchingDefault(emptyList()) { SystemFileSystem.list(this).toList() }

fun Path.mkdir(): Boolean = catchingDefault(false) {
    SystemFileSystem.createDirectories(this, mustCreate = false)
    true
}

fun Path.delete(): Boolean = catchingDefault(false) {
    SystemFileSystem.delete(this, mustExist = false)
    true
}

fun Path.deleteRecursively(): Boolean = catchingDefault(false) {
    if (!SystemFileSystem.exists(this)) throw FileNotFoundException("File does not exist: $this")
    val queue = ArrayDeque<Path>()
    queue.add(this)
    while (queue.isNotEmpty()) {
        val currentPath = queue.first()
        val metadata = SystemFileSystem.metadataOrNull(currentPath)
        when {
            metadata == null -> throw IOException("Path is neither a file nor a directory: $this")
            metadata.isRegularFile -> {
                SystemFileSystem.delete(currentPath, mustExist = false)
                queue.removeFirst()
            }
            metadata.isDirectory -> {
                val list = SystemFileSystem.list(currentPath)
                if (list.isEmpty()) {
                    SystemFileSystem.delete(currentPath, mustExist = false)
                    queue.removeFirst()
                } else queue.addAll(0, list)
            }
        }
    }
    true
}

inline fun <reified R> Path.read(block: (Source) -> R): R = SystemFileSystem.source(this).buffered().use(block)

fun Path.readText(): String? = catchingNull { SystemFileSystem.source(this).buffered().use { it.readString() } }

fun Path.readByteArray(): ByteArray? = catchingNull { SystemFileSystem.source(this).buffered().use { it.readByteArray() } }

inline fun Path.write(block: (Sink) -> Unit) { SystemFileSystem.sink(this).buffered().use(block) }

fun Path.writeText(text: String): Boolean = catchingDefault(false) {
    SystemFileSystem.sink(this).buffered().use { it.writeString(text) }
    true
}

fun Path.writeByteArray(data: ByteArray): Boolean = catchingDefault(false) {
    SystemFileSystem.sink(this).buffered().use { it.write(data) }
    true
}