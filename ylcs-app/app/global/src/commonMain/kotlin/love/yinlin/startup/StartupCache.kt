package love.yinlin.startup

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Sink
import love.yinlin.compose.cache.DiskCache
import love.yinlin.compose.cache.UrlCacheSource
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.NetClient
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.foundation.StartupArg
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.SyncStartup
import love.yinlin.fs.File

@StartupArg(index = 0, name = "cachePath", type = File::class)
@Stable
class StartupCache(context: PlatformContextProvider) : SyncStartup(context) {
    private var diskCache: DiskCache<UrlCacheSource>? = null

    override fun init(scope: CoroutineScope, args: StartupArgs) {
        val cachePath: File = args[0]
        diskCache = object : DiskCache<UrlCacheSource>(cachePath) {
            override suspend fun fetch(source: UrlCacheSource, sink: Sink) {
                Coroutines.io { NetClient.File.download(source.url, sink) }
            }
        }
    }

    suspend fun store(url: String): File? = diskCache?.store(UrlCacheSource(url))
    suspend fun loadByteArray(url: String): ByteArray? = diskCache?.loadByteArray(UrlCacheSource(url))
}