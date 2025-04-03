package love.yinlin.platform

object OS {
	val platform: Platform = osPlatform // 平台

	object Net {
		fun openUrl(url: String) = osNetOpenUrl(url)
	}

	object Storage {
		val cachePath: String by lazy { osStorageCachePath }
		val cacheSize get() = osStorageCacheSize
		fun clearCache() = osStorageClearCache()
	}
}

// ----  OS

internal expect val osPlatform: Platform

// ------------  Net

internal expect fun osNetOpenUrl(url: String)

// ------------  Storage

internal expect val osStorageCachePath: String
internal expect val osStorageCacheSize: Long
internal expect fun osStorageClearCache()