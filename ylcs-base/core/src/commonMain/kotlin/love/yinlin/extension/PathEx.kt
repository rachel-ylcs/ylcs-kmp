package love.yinlin.extension

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString
import love.yinlin.io.Sources
import love.yinlin.io.safeToSources

val Path.extension: String get() = this.name.substringAfterLast('.')

val Path.nameWithoutExtension: String get() = this.name.substringBeforeLast('.')

val Path.exists: Boolean get() = SystemFileSystem.exists(this)

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

fun Path.rename(filename: String): Path? = catchingNull {
    val parent = this.parent
    val newPath = if (parent == null) Path(this.toString().replace(this.name, filename)) else Path(parent, filename)
    SystemFileSystem.atomicMove(this, newPath)
    newPath
}

val Path.size: Long get() = catchingDefault(0L) {
    var size = 0L
    val queue = ArrayDeque<Path>()
    queue.add(this)
    while (queue.isNotEmpty()) {
        val front = queue.removeFirst()
        val metadata = SystemFileSystem.metadataOrNull(front)
        when {
            metadata == null -> {}
            metadata.isRegularFile -> size += metadata.size
            metadata.isDirectory -> queue.addAll(SystemFileSystem.list(front))
        }
    }
    size
}

fun Path.deleteRecursively(): Boolean = catchingDefault(false) {
    val stack = ArrayDeque<Path>()
    stack.add(this)
    while (stack.isNotEmpty()) {
        val top = stack.last()
        val metadata = SystemFileSystem.metadataOrNull(top)
        when {
            metadata == null -> stack.removeLast()
            metadata.isRegularFile -> {
                SystemFileSystem.delete(top, mustExist = false)
                stack.removeLast()
            }
            metadata.isDirectory -> {
                val list = SystemFileSystem.list(top)
                if (list.isEmpty()) {
                    SystemFileSystem.delete(top, mustExist = false)
                    stack.removeLast()
                }
                else stack.addAll(list)
            }
        }
    }
    true
}

val Path.rawSource: RawSource get() = SystemFileSystem.source(this)
val Path.rawSink: RawSink get() = SystemFileSystem.sink(this)
val Path.bufferedSource: Source get() = SystemFileSystem.source(this).buffered()
val Path.bufferedSink: Sink get() = SystemFileSystem.sink(this).buffered()

suspend inline fun <R> Path.read(crossinline block: suspend (Source) -> R): R = SystemFileSystem.source(this@read).buffered().use { block(it) }

suspend fun Path.readText(): String? = catchingNull { read { it.readString() } }

suspend fun Path.readByteArray(): ByteArray? = catchingNull { read { it.readByteArray() } }

suspend inline fun Path.write(crossinline block: suspend (Sink) -> Unit) = SystemFileSystem.sink(this@write).buffered().use { block(it) }

suspend fun Path.writeText(text: String): Boolean = catchingDefault(false) {
    write { it.writeString(text) }
    true
}

suspend fun Path.writeByteArray(data: ByteArray): Boolean = catchingDefault(false) {
    write { it.write(data) }
    true
}

suspend fun Path.writeTo(other: Path): Boolean = catchingDefault(false) {
    this.read { source ->
        other.write { sink ->
            source.transferTo(sink)
        }
    }
    true
}

fun List<Path>.safeRawSources(): Sources<RawSource>? = this.safeToSources { SystemFileSystem.source(it) }
fun List<Path>.safeSources(): Sources<Source>? = this.safeToSources { SystemFileSystem.source(it).buffered() }