package love.yinlin.compose.cache

import kotlinx.coroutines.NonCancellable
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.writeString
import love.yinlin.concurrent.Mutex
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.cleaning
import love.yinlin.fs.File

/**
 * 同目录临时文件、关闭流后原子 rename、读取与提交互斥。每个目录只允许一个实例拥有。
 * 写入回调不持有元数据锁，已有条目可在其他下载期间命中；删除和清理仍与写入互斥。
 * Persistent 保留原文件名并可读取旧 DiskCache 数据；Lru 目录必须独占。
 * Lru 以小型访问日志恢复重启后的顺序，日志定期压缩，容量预算计算数据文件。
 */
class FileDiskStorage(
    private val directory: File,
    private val policy: DiskPolicy = DiskPolicy.Persistent
) : DiskStorage {
    private val writeMutex = Mutex()
    private val mutex = Mutex()
    private val entries = LinkedHashMap<String, Long>()
    private var initialized = false
    private var bytes = 0L
    private var journalLines = 0
    private val journal get() = File(directory, ".lru")

    private fun checkKey(key: String) {
        require(key.length in 1 .. 180 && key != "." && key != ".." && !key.startsWith('.'))
        require(key.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }) { "Cache key must be a file name" }
    }

    private suspend fun initialize() {
        if (initialized) {
            if (directory.isDirectory() && (policy !is DiskPolicy.Lru || journal.exists())) return
            entries.clear()
            bytes = 0L
            journalLines = 0
            initialized = false
        }

        directory.mkdir()

        if (policy is DiskPolicy.Lru) {
            for (file in directory.list()) {
                if (file.name.startsWith('.') && file.name.endsWith(".tmp")) file.delete()
                else if (!file.name.startsWith('.') && file.isFile()) {
                    checkKey(file.name)
                    entries[file.name] = file.fileSize()
                }
            }

            val order = if (journal.fileSize() in 1L .. 1024 * 1024) journal.readText()?.lines() else null

            for (key in order ?: []) {
                val size = entries.remove(key)
                if (size != null) entries[key] = size
            }

            bytes = entries.values.sum()
            trim()
            compactJournal()
        }
        initialized = true
    }

    private suspend fun compactJournal() {
        val temporary = File(directory, ".lru.tmp")
        val text = entries.keys.joinToString(separator = "\n", postfix = if (entries.isEmpty()) "" else "\n")

        temporary.writeSync { it.writeString(text) }
        temporary.move(journal)
        check(!temporary.exists() && journal.fileSize() == text.encodeToByteArray().size.toLong()) { "Cache index rename failed" }
        journalLines = entries.size
    }

    private suspend fun touch(key: String, size: Long) {
        entries.remove(key)?.let { bytes -= it }
        entries[key] = size
        bytes += size

        if (policy is DiskPolicy.Lru) {
            journal.rawSink(append = true).buffered().use { it.writeString("$key\n") }
            journalLines++
            if (journalLines > entries.size * 2 + 64) compactJournal()
        }
    }

    private suspend fun removeEntry(key: String) {
        val file = File(directory, key)
        file.delete()
        check(!file.exists()) { "Cache delete failed" }

        entries.remove(key)?.let { bytes -= it }
    }

    private suspend fun trim() {
        val limit = (policy as? DiskPolicy.Lru)?.maxBytes ?: return

        while ((bytes > limit || entries.size > 10000) && entries.isNotEmpty()) {
            removeEntry(entries.keys.first())
        }
    }

    override suspend fun <T : Any> read(key: String, block: (Source) -> T): T? = mutex.with {
        Coroutines.io {
            checkKey(key)
            initialize()

            val file = File(directory, key)
            if (file.fileSize() <= 0L) {
                removeEntry(key)
                return@io null
            }

            val value = file.read { block(it) }
            touch(key, file.fileSize())
            value
        }
    }

    override suspend fun write(key: String, block: suspend (Sink) -> Unit): File? = store(key, false, block)

    override suspend fun getOrWrite(key: String, block: suspend (Sink) -> Unit): File? = store(key, true, block)

    private suspend fun findExisting(key: String): File? {
        val file = File(directory, key)
        val size = file.fileSize()
        if (size <= 0L) return null
        touch(key, size)
        return file
    }

    private suspend fun store(key: String, existing: Boolean, block: suspend (Sink) -> Unit): File? = Coroutines.io {
        checkKey(key)
        if (existing) {
            val cached = mutex.with {
                initialize()
                findExisting(key)
            }
            if (cached != null) return@io cached
        }

        writeMutex.with writing@{
            val cached = mutex.with {
                initialize()
                if (existing) findExisting(key) else null
            }
            if (cached != null) return@writing cached

            val target = File(directory, key)
            val temporary = File(directory, ".$key.tmp")

            cleaning({
                temporary.write(block)
                val size = temporary.fileSize()
                require(size > 0) { "Empty cache entry" }

                if (policy is DiskPolicy.Lru && size > policy.maxBytes) return@cleaning null

                mutex.with {
                    temporary.move(target)
                    check(!temporary.exists() && target.fileSize() == size) { "Cache rename failed" }

                    touch(key, size)
                    trim()
                    target
                }
            }) {
                Coroutines.with(NonCancellable) {
                    temporary.delete()
                }
            }
        }
    }

    override suspend fun remove(key: String) = writeMutex.with {
        mutex.with {
            Coroutines.io {
                checkKey(key)
                initialize()
                removeEntry(key)
            }
        }
    }

    override suspend fun clear() = writeMutex.with {
        mutex.with {
            Coroutines.io {
                initialize()
                for (key in entries.keys.toList()) removeEntry(key)
                if (policy is DiskPolicy.Lru) compactJournal()
            }
        }
    }
}