package love.yinlin.platform

import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingNull
import love.yinlin.service.PlatformContext

abstract class OSStorage {
    abstract val dataPath: Path
    abstract val cachePath: Path
    abstract val cacheSize: Long
    abstract fun clearCache()

    suspend inline fun createTempFile(crossinline block: suspend (Sink) -> Boolean): Path? = catchingNull {
        val path = Path(cachePath, DateEx.CurrentLong.toString())
        require(SystemFileSystem.sink(path).buffered().use { Coroutines.io { block(it) } })
        path
    }

    suspend fun createTempFolder(): Path? = catchingNull {
        Coroutines.io {
            val path = Path(cachePath, DateEx.CurrentLong.toString())
            SystemFileSystem.createDirectories(path)
            path
        }
    }
}

expect fun buildOSStorage(context: PlatformContext, appName: String): OSStorage