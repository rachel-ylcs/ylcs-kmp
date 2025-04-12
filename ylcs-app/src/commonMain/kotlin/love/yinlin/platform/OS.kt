package love.yinlin.platform

import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.extension.DateEx

object OS {
	val platform: Platform = osPlatform // 平台

	object Net {
		fun openUrl(url: String) = osNetOpenUrl(url)
	}

	object Storage {
		val dataPath: Path by lazy { osStorageDataPath }

		val musicPath: Path by lazy { Path(dataPath, "music") }

		val cachePath: Path by lazy { osStorageCachePath }
		val cacheSize get() = osStorageCacheSize
		fun clearCache() = osStorageClearCache()

		suspend inline fun createTempFile(crossinline block: suspend (Sink) -> Boolean): Path? = try {
			val path = Path(cachePath, DateEx.CurrentLong.toString())
			if (SystemFileSystem.sink(path).buffered().use {
				Coroutines.io { block(it) }
			}) path else null
		}
		catch (_: Throwable) { null }
	}
}

// ----  OS

internal expect val osPlatform: Platform

// ------------  Net

internal expect fun osNetOpenUrl(url: String)

// ------------  Storage

internal expect val osStorageDataPath: Path
internal expect val osStorageCachePath: Path
internal expect val osStorageCacheSize: Long
internal expect fun osStorageClearCache()