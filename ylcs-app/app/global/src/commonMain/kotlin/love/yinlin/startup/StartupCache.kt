package love.yinlin.startup

import androidx.compose.runtime.Stable
import love.yinlin.compose.cache.DiskCache
import love.yinlin.compose.cache.XXHash64
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.Startup
import love.yinlin.foundation.StartupFactory
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.fs.File

@Stable
class StartupCache(pool: StartupPool, private val cachePath: File) : Startup(pool) {
    class Factory(private val cachePath: File) : StartupFactory<StartupCache> {
        override val id: String = StartupID<StartupCache>()
        override val dependencies: List<String> = emptyList()
        override fun build(pool: StartupPool): StartupCache = StartupCache(pool, cachePath)
    }

    private var diskCache: DiskCache<String>? = null

    override suspend fun init() {
        diskCache = DiskCache(
            cachePath = cachePath,
            key = XXHash64::hash
        ) { source, sink ->
            Coroutines.io { NetClient.File.download(source, sink) }
        }
    }

    suspend fun store(url: String): File? = diskCache?.store(url)
    suspend fun loadByteArray(url: String): ByteArray? = diskCache?.loadByteArray(url)
}