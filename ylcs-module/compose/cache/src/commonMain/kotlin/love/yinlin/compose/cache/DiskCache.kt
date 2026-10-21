package love.yinlin.compose.cache

import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.extension.catchingNull
import love.yinlin.fs.File

class DiskCache<S : Any>(
    cachePath: File,
    private val key: (S) -> Any =  { it },
    private val fetcher: suspend (source: S, sink: Sink) -> Unit
) {
    private val storage: DiskStorage = FileDiskStorage(cachePath, DiskPolicy.Persistent)

    /**
     * 存储指定数据源，如果本地存在缓存则直接返回对应路径，否则提取后再返回
     */
    suspend fun store(source: S): File? = catchingNull {
        storage.getOrWrite(key(source).toString()) {
            fetcher(source, it)
        }
    }

    /**
     * 读取指定数据源，如果本地存在缓存则直接返回对应数据，否则提取后再返回
     */
    suspend fun load(source: S): Source? = store(source)?.bufferedSource()

    /**
     * 以字节形式读取指定数据源，如果本地存在缓存则直接返回对应数据，否则提取后再返回
     */
    suspend fun loadString(source: S): String? = store(source)?.readText()

    /**
     * 以字节形式读取指定数据源，如果本地存在缓存则直接返回对应数据，否则提取后再返回
     */
    suspend fun loadByteArray(source: S): ByteArray? = store(source)?.readByteArray()
}