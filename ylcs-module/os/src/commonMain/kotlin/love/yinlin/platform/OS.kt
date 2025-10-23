package love.yinlin.platform

import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.uri.Uri
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingNull

data object OS {
    data object Application {
        suspend fun startAppIntent(uri: Uri): Boolean = osApplicationStartAppIntent(uri)
        fun copyText(text: String): Boolean = osApplicationCopyText(text)
    }

    data object Net {
        fun openUrl(url: String) {
            Uri.parse(url)?.let { osNetOpenUrl(it) }
        }
    }

    data object Storage {
        val dataPath: Path by lazy { osStorageDataPath }

        val musicPath: Path by lazy { Path(dataPath, "music") }

        val cachePath: Path by lazy { osStorageCachePath }
        val cacheSize get() = osStorageCacheSize
        fun clearCache() = osStorageClearCache()

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
}

// ------------  Application

internal expect suspend fun osApplicationStartAppIntent(uri: Uri): Boolean
internal expect fun osApplicationCopyText(text: String): Boolean

// ------------  Net

internal expect fun osNetOpenUrl(uri: Uri)

// ------------  Storage

internal expect val osStorageDataPath: Path
internal expect val osStorageCachePath: Path
internal expect val osStorageCacheSize: Long
internal expect fun osStorageClearCache()