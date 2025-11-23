package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.io.Sink
import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingNull
import love.yinlin.extension.mkdir
import love.yinlin.extension.write

@Stable
abstract class OSStorage {
    abstract val appPath: Path
    abstract val dataPath: Path
    abstract val cachePath: Path
    abstract suspend fun calcCacheSize(): Long
    abstract suspend fun clearCache()

    suspend inline fun createTempFile(filename: String? = null, crossinline block: suspend (Sink) -> Boolean): Path? = catchingNull {
        val name = filename ?: DateEx.CurrentLong.toString()
        Path(cachePath, name).apply {
            Coroutines.io {
                write { require(block(it)) }
            }
        }
    }

    suspend fun createTempFolder(filename: String? = null): Path? = catchingNull {
        val name = filename ?: DateEx.CurrentLong.toString()
        Path(cachePath, name).apply {
            Coroutines.io { mkdir() }
        }
    }
}

@Stable
expect fun buildOSStorage(context: Context, appName: String): OSStorage