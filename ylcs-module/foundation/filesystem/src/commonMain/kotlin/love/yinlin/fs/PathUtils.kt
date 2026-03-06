package love.yinlin.fs

import kotlinx.io.*
import kotlinx.io.files.Path
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.IOCoroutine
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
@IOCoroutine
suspend fun Path.exists(): Boolean = Coroutines.io { PlatformFileSystem.exists(this@exists) }

/**
 * 是否是普通文件
 */
@IOCoroutine
suspend fun Path.isFile(): Boolean = Coroutines.io { PlatformFileSystem.metadataOrNull(this@isFile)?.isRegularFile ?: false }

/**
 * 是否是目录
 */
@IOCoroutine
suspend fun Path.isDirectory(): Boolean = Coroutines.io { PlatformFileSystem.metadataOrNull(this@isDirectory)?.isDirectory ?: false }

/**
 * 子目录
 */
@IOCoroutine
suspend fun Path.list(): List<Path> = catchingDefault(emptyList()) {
    Coroutines.io {
        PlatformFileSystem.list(this@list).toList()
    }
}

/**
 * 创建文件夹，支持多级目录
 */
@IOCoroutine
suspend fun Path.mkdir(): Boolean = catchingDefault(false) {
    Coroutines.io {
        PlatformFileSystem.createDirectories(this@mkdir, mustCreate = false)
        true
    }
}

/**
 * 删除文件
 */
@IOCoroutine
suspend fun Path.delete(): Boolean = catchingDefault(false) {
    Coroutines.io {
        PlatformFileSystem.delete(this@delete, mustExist = false)
        true
    }
}

/**
 * 删除文件或目录
 */
@IOCoroutine
suspend fun Path.deleteRecursively(): Boolean = catchingDefault(false) {
    Coroutines.io {
        val stack = ArrayDeque<Path>()
        stack.add(this@deleteRecursively)
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
}

/**
 * 移动
 */
@IOCoroutine
suspend fun Path.move(dest: Path): Boolean = catchingDefault(false) {
    Coroutines.io {
        PlatformFileSystem.atomicMove(this@move, dest)
        true
    }
}

/**
 * 重命名
 */
@IOCoroutine
suspend fun Path.rename(filename: String): Path? = catchingNull {
    Coroutines.io {
        val path = this@rename
        val parent = path.parent
        val newPath = if (parent == null) Path(this.toString().replace(path.name, filename)) else Path(parent, filename)
        PlatformFileSystem.atomicMove(path, newPath)
        newPath
    }
}

/**
 * 文件大小
 */
@IOCoroutine
suspend fun Path.fileSize(): Long = catchingDefault(0L) {
    Coroutines.io {
        val metadata = PlatformFileSystem.metadataOrNull(this@fileSize)
        if (metadata?.isRegularFile == true) metadata.size else 0L
    }
}

/**
 * 文件或目录大小
 */
@IOCoroutine
suspend fun Path.size(): Long = catchingDefault(0L) {
    Coroutines.io {
        var size = 0L
        val metadata = PlatformFileSystem.metadataOrNull(this@size)
        when {
            metadata == null -> {}
            metadata.isRegularFile -> size += metadata.size
            metadata.isDirectory -> {
                val queue = ArrayDeque<Path>()
                queue.add(this@size)
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
}

@IOCoroutine
suspend fun Path.rawSource(): RawSource = Coroutines.io { PlatformFileSystem.source(this@rawSource) }

@IOCoroutine
suspend fun Path.rawSink(): RawSink = Coroutines.io { PlatformFileSystem.sink(this@rawSink) }

@IOCoroutine
suspend fun Path.bufferedSource(): Source = Coroutines.io { PlatformFileSystem.source(this@bufferedSource).buffered() }

@IOCoroutine
suspend fun Path.bufferedSink(): Sink = Coroutines.io { PlatformFileSystem.sink(this@bufferedSink).buffered() }

@IOCoroutine
suspend fun List<Path>.safeRawSources(): Sources<RawSource>? = Coroutines.io {
    this@safeRawSources.safeToSources { PlatformFileSystem.source(it) }
}

@IOCoroutine
suspend fun List<Path>.safeSources(): Sources<Source>? = Coroutines.io {
    this@safeSources.safeToSources { PlatformFileSystem.source(it).buffered() }
}

/**
 * 读文件
 */
@IOCoroutine
suspend inline fun <R> Path.read(@IOCoroutine crossinline block: suspend (Source) -> R): R = Coroutines.io {
    PlatformFileSystem.source(this@read).buffered().use { block(it) }
}

/**
 * 读文本文件
 */
@IOCoroutine
suspend fun Path.readText(): String? = catchingNull { read { it.readString() } }

/**
 * 读字节文件
 */
@IOCoroutine
suspend fun Path.readByteArray(): ByteArray? = catchingNull { read { it.readByteArray() } }

/**
 * 写文件
 */
@IOCoroutine
suspend inline fun Path.write(@IOCoroutine crossinline block: suspend (Sink) -> Unit) = Coroutines.io {
    PlatformFileSystem.sink(this@write).buffered().use { block(it) }
}

/**
 * 写文本文件
 */
@IOCoroutine
suspend fun Path.writeText(text: String): Boolean = catchingDefault(false) {
    write { it.writeString(text) }
    true
}

/**
 * 写字节文件
 */
@IOCoroutine
suspend fun Path.writeByteArray(data: ByteArray): Boolean = catchingDefault(false) {
    write { it.write(data) }
    true
}

/**
 * 写入文件
 */
@IOCoroutine
suspend fun Path.writeTo(other: Path): Boolean = catchingDefault(false) {
    this.read { source ->
        other.write { sink ->
            source.transferTo(sink)
        }
    }
    true
}