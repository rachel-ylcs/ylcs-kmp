package love.yinlin.compose.cache

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readString
import love.yinlin.concurrent.Mutex
import love.yinlin.extension.catchingNull
import love.yinlin.fs.File

class DiskCache<S : Any>(
    private val cachePath: File,
    private val key: (S) -> Any =  { it },
    private val fetcher: suspend (source: S, sink: Sink) -> Unit
) {
    private val mutex = Mutex()

    private suspend fun check(target: File): File? {
        val metadata = target.metadata()
        if (metadata != null && metadata.isRegularFile && metadata.size > 0L) return target
        return null
    }

    /**
     * 存储指定数据源，如果本地存在缓存则直接返回对应路径，否则提取后再返回
     */
    suspend fun store(source: S): File? {
        val sourceKey = key(source).toString()
        val target = File(cachePath, sourceKey)
        // 检查缓存是否存在
        if (check(target) != null) return target

        // 提取
        return mutex.with {
            if (check(target) != null) target
            else {
                val temp = File(cachePath, "$sourceKey.tmp")
                try {
                    cachePath.mkdir()
                    temp.write { sink -> fetcher(source, sink) }
                    if (check(temp) != null) {
                        temp.move(target)
                        target
                    }
                    else error("")
                } catch (_: Throwable) {
                    temp.delete()
                    null
                }
            }
        }
    }

    /**
     * 读取指定数据源，如果本地存在缓存则直接返回对应数据，否则提取后再返回
     */
    suspend fun load(source: S): Source? = store(source)?.bufferedSource()

    /**
     * 以字节形式读取指定数据源，如果本地存在缓存则直接返回对应数据，否则提取后再返回
     */
    suspend fun loadString(source: S): String? = catchingNull { load(source)?.readString() }

    /**
     * 以字节形式读取指定数据源，如果本地存在缓存则直接返回对应数据，否则提取后再返回
     */
    suspend fun loadByteArray(source: S): ByteArray? = catchingNull { load(source)?.readByteArray() }
}