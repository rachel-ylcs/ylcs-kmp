package love.yinlin.platform

import kotlinx.io.Sink
import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingNull
import love.yinlin.extension.mkdir
import love.yinlin.extension.write

abstract class OSStorage {
    abstract val appPath: Path
    abstract val dataPath: Path
    abstract val cachePath: Path
    abstract suspend fun calcCacheSize(): Long
    abstract suspend fun clearCache()

    suspend inline fun createTempFile(crossinline block: suspend (Sink) -> Boolean): Path? = catchingNull {
        Path(cachePath, DateEx.CurrentLong.toString()).apply {
            Coroutines.io {
                write { require(block(it)) }
            }
        }
    }

    suspend fun createTempFolder(): Path? = catchingNull {
        Path(cachePath, DateEx.CurrentLong.toString()).apply {
            Coroutines.io {
                mkdir()
            }
        }
    }
}

expect fun buildOSStorage(context: Context, appName: String): OSStorage