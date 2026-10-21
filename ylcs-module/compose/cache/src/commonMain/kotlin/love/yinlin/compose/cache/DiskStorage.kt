package love.yinlin.compose.cache

import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.fs.File

/**
 * 单一所有者的磁盘存储。read 的流只在 block 内有效；写入成功才原子替换旧值。
 * key 是文件名，不是路径。Lru 返回的文件只适合立即使用，长期持有请使用 Persistent。
 */
interface DiskStorage {
    suspend fun <T : Any> read(key: String, block: (Source) -> T): T?
    suspend fun write(key: String, block: suspend (Sink) -> Unit): File?
    suspend fun getOrWrite(key: String, block: suspend (Sink) -> Unit): File?
    suspend fun remove(key: String)
    suspend fun clear()
}