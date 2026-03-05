package love.yinlin.fs

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import love.yinlin.io.Sources
import love.yinlin.io.safeToSources

/**
 * 拓展名(不含点)
 */
val Path.extension: String get() = this.name.substringAfterLast('.')

/**
 * 文件名(不含拓展名)
 */
val Path.nameWithoutExtension: String get() = this.name.substringBeforeLast('.')

/**
 * 是否存在
 */
val Path.exists: Boolean get() = PlatformFileSystem.exists(this)

/**
 * 是否是普通文件
 */
val Path.isFile: Boolean get() = PlatformFileSystem.metadataOrNull(this)?.isRegularFile ?: false

/**
 * 是否是目录
 */
val Path.isDirectory: Boolean get() = PlatformFileSystem.metadataOrNull(this)?.isDirectory ?: false

/**
 * 子目录
 */
fun Path.list(): List<Path> = catchingDefault(emptyList()) { PlatformFileSystem.list(this).toList() }

/**
 * 创建文件夹，支持多级目录
 */
fun Path.mkdir(): Boolean = catchingDefault(false) {
    PlatformFileSystem.createDirectories(this, mustCreate = false)
    true
}

/**
 * 删除文件
 */
fun Path.delete(): Boolean = catchingDefault(false) {
    PlatformFileSystem.delete(this, mustExist = false)
    true
}

/**
 * 删除文件或目录
 */
fun Path.deleteRecursively(): Boolean = catchingDefault(false) {
    val stack = ArrayDeque<Path>()
    stack.add(this)
    while (stack.isNotEmpty()) {
        val top = stack.last()
        val metadata = PlatformFileSystem.metadataOrNull(top)
        when {
            metadata == null -> stack.removeAt(stack.lastIndex)
            metadata.isRegularFile -> {
                PlatformFileSystem.delete(top, mustExist = false)
                stack.removeAt(stack.lastIndex)
            }
            metadata.isDirectory -> {
                val list = PlatformFileSystem.list(top)
                if (list.isEmpty()) {
                    PlatformFileSystem.delete(top, mustExist = false)
                    stack.removeAt(stack.lastIndex)
                }
                else stack.addAll(list)
            }
        }
    }
    true
}

/**
 * 移动
 */
fun Path.move(dest: Path): Boolean = catchingDefault(false) {
    PlatformFileSystem.atomicMove(this, dest)
    true
}

/**
 * 重命名
 */
fun Path.rename(filename: String): Path? = catchingNull {
    val parent = this.parent
    val newPath = if (parent == null) Path(this.toString().replace(this.name, filename)) else Path(parent, filename)
    PlatformFileSystem.atomicMove(this, newPath)
    newPath
}

/**
 * 文件大小
 */
val Path.fileSize: Long get() = catchingDefault(0L) {
    val metadata = PlatformFileSystem.metadataOrNull(this)
    if (metadata?.isRegularFile == true) metadata.size else 0L
}

/**
 * 文件或目录大小
 */
val Path.size: Long get() = catchingDefault(0L) {
    var size = 0L
    val metadata = PlatformFileSystem.metadataOrNull(this)
    when {
        metadata == null -> {}
        metadata.isRegularFile -> size += metadata.size
        metadata.isDirectory -> {
            val queue = ArrayDeque<Path>()
            queue.add(this)
            while (queue.isNotEmpty()) {
                val front = queue.removeAt(0)
                val metadata = PlatformFileSystem.metadataOrNull(front)
                when {
                    metadata == null -> {}
                    metadata.isRegularFile -> size += metadata.size
                    metadata.isDirectory -> queue.addAll(PlatformFileSystem.list(front))
                }
            }
        }
    }
    size
}

val Path.rawSource: RawSource get() = PlatformFileSystem.source(this)

val Path.rawSink: RawSink get() = PlatformFileSystem.sink(this)

val Path.bufferedSource: Source get() = PlatformFileSystem.source(this).buffered()

val Path.bufferedSink: Sink get() = PlatformFileSystem.sink(this).buffered()

fun List<Path>.safeRawSources(): Sources<RawSource>? = this.safeToSources { PlatformFileSystem.source(it) }

fun List<Path>.safeSources(): Sources<Source>? = this.safeToSources { PlatformFileSystem.source(it).buffered() }

/**
 * 读文件
 */
suspend inline fun <R> Path.read(crossinline block: suspend (Source) -> R): R = PlatformFileSystem.source(this@read).buffered().use { block(it) }

/**
 * 读文本文件
 */
suspend fun Path.readText(): String? = catchingNull { read { it.readString() } }

/**
 * 读字节文件
 */
suspend fun Path.readByteArray(): ByteArray? = catchingNull { read { it.readByteArray() } }

/**
 * 写文件
 */
suspend inline fun Path.write(crossinline block: suspend (Sink) -> Unit) = PlatformFileSystem.sink(this@write).buffered().use { block(it) }

/**
 * 写文本文件
 */
suspend fun Path.writeText(text: String): Boolean = catchingDefault(false) {
    write { it.writeString(text) }
    true
}

/**
 * 写字节文件
 */
suspend fun Path.writeByteArray(data: ByteArray): Boolean = catchingDefault(false) {
    write { it.write(data) }
    true
}

/**
 * 写入文件
 */
suspend fun Path.writeTo(other: Path): Boolean = catchingDefault(false) {
    this.read { source ->
        other.write { sink ->
            source.transferTo(sink)
        }
    }
    true
}