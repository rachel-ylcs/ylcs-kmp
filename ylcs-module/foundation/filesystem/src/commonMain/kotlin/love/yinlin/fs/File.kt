package love.yinlin.fs

import kotlinx.io.*
import kotlinx.io.files.FileMetadata
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.IOCoroutine
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import love.yinlin.io.Sources
import love.yinlin.io.safeToSources

@Serializable(File.FileSerializer::class)
abstract class File {
    companion object {
        operator fun invoke(uri: String): File = buildFile(uri)
        operator fun invoke(uri: String, vararg parts: String): File = buildFile(File(uri), *parts)
        operator fun invoke(parent: File, vararg parts: String): File = buildFile(parent, *parts)
    }

    object FileSerializer : KSerializer<File> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.File", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: File) = encoder.encodeString(value.path)
        override fun deserialize(decoder: Decoder): File = File(decoder.decodeString())
    }

    /**
     * 文件名
     */
    abstract val name: String

    /**
     * 是否是绝对路径
     */
    abstract val isAbsolute: Boolean

    /**
     * 父目录
     */
    abstract val parent: File?

    /**
     * 元信息
     */
    @IOCoroutine
    abstract suspend fun metadata(): FileMetadata?

    /**
     * 删除文件
     */
    @IOCoroutine
    abstract suspend fun delete()

    /**
     * 创建文件夹，支持多级目录
     */
    @IOCoroutine
    abstract suspend fun mkdir()

    /**
     * 移动
     */
    @IOCoroutine
    abstract suspend fun move(dst: File)

    @IOCoroutine
    abstract suspend fun rawSource(): RawSource

    @IOCoroutine
    abstract suspend fun rawSink(append: Boolean = false): RawSink

    /**
     * 子目录
     */
    @IOCoroutine
    abstract suspend fun list(): List<File>

    /**
     * 路径
     */
    val path: String get() = this.toString()

    /**
     * 拓展名(不含点)
     */
    val extension: String get() = this.name.substringAfterLast('.')

    /**
     * 文件名(不含拓展名)
     */
    val nameWithoutExtension: String get() = this.name.substringBeforeLast('.')

    /**
     * 是否存在
     */
    @IOCoroutine
    suspend fun exists(): Boolean = metadata() != null

    /**
     * 是否是普通文件
     */
    @IOCoroutine
    suspend fun isFile(): Boolean = metadata()?.isRegularFile ?: false

    /**
     * 是否是目录
     */
    @IOCoroutine
    suspend fun isDirectory(): Boolean = metadata()?.isDirectory ?: false

    /**
     * 文件大小
     */
    @IOCoroutine
    suspend fun fileSize(): Long = catchingDefault(0L) {
        val fileInfo = metadata()
        if (fileInfo?.isRegularFile == true) fileInfo.size else 0L
    }

    /**
     * 文件或目录大小
     */
    @IOCoroutine
    suspend fun size(): Long = catchingDefault(0L) {
        val current = this
        Coroutines.io {
            var size = 0L
            val fileInfo = current.metadata()
            when {
                fileInfo == null -> {}
                fileInfo.isRegularFile -> size += fileInfo.size
                fileInfo.isDirectory -> {
                    val queue = ArrayDeque<File>()
                    queue.add(current)
                    while (queue.isNotEmpty()) {
                        val front = queue.removeAt(0)
                        val frontInfo = front.metadata()
                        when {
                            frontInfo == null -> {}
                            frontInfo.isRegularFile -> size += frontInfo.size
                            frontInfo.isDirectory -> queue.addAll(front.list())
                        }
                    }
                }
            }
            size
        }
    }

    /**
     * 删除文件或目录
     */
    @IOCoroutine
    suspend fun deleteRecursively(): Boolean = catchingDefault(false) {
        val stack = ArrayDeque<File>()
        stack.add(this)
        Coroutines.io {
            while (stack.isNotEmpty()) {
                val top = stack.last()
                val fileInfo = top.metadata()
                when {
                    fileInfo == null -> stack.removeAt(stack.lastIndex)
                    fileInfo.isRegularFile -> {
                        top.delete()
                        stack.removeAt(stack.lastIndex)
                    }
                    fileInfo.isDirectory -> {
                        val list = top.list()
                        if (list.isEmpty()) {
                            top.delete()
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
     * 重命名
     */
    @IOCoroutine
    suspend fun rename(filename: String): File? = catchingNull {
        val current = this
        val newFile = current.parent?.let { File(it, filename) } ?: File(filename)
        move(newFile)
        newFile
    }

    @IOCoroutine
    suspend fun bufferedSource(): Source = rawSource().buffered()

    @IOCoroutine
    suspend fun bufferedSink(): Sink = rawSink().buffered()

    /**
     * 读文件
     */
    @IOCoroutine
    suspend inline fun <R> read(@IOCoroutine crossinline block: suspend (Source) -> R): R = Coroutines.io {
        bufferedSource().use { block(it) }
    }

    /**
     * 读文本文件
     */
    @IOCoroutine
    suspend fun readText(): String? = catchingNull { read { it.readString() } }

    /**
     * 读字节文件
     */
    @IOCoroutine
    suspend fun readByteArray(): ByteArray? = catchingNull { read { it.readByteArray() } }

    /**
     * 写文件
     */
    @IOCoroutine
    suspend inline fun write(@IOCoroutine crossinline block: suspend (Sink) -> Unit) = Coroutines.io {
        bufferedSink().use { block(it) }
    }

    /**
     * 写文本文件
     */
    @IOCoroutine
    suspend fun writeText(text: String): Boolean = catchingDefault(false) {
        write { it.writeString(text) }
        true
    }

    /**
     * 写字节文件
     */
    @IOCoroutine
    suspend fun writeByteArray(data: ByteArray): Boolean = catchingDefault(false) {
        write { it.write(data) }
        true
    }

    /**
     * 写入文件
     */
    @IOCoroutine
    suspend fun writeTo(other: File): Boolean = catchingDefault(false) {
        read { source ->
            other.write { sink ->
                source.transferTo(sink)
            }
        }
        true
    }
}

expect fun buildFile(uri: String): File
expect fun buildFile(parent: File, vararg parts: String): File

@IOCoroutine
suspend fun Collection<File>.safeRawSources(): Sources<RawSource>? = Coroutines.io {
    this@safeRawSources.safeToSources { it.rawSource() }
}

@IOCoroutine
suspend fun Collection<File>.safeSources(): Sources<Source>? = Coroutines.io {
    this@safeSources.safeToSources { it.rawSource().buffered() }
}