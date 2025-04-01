package love.yinlin.platform

import androidx.compose.ui.graphics.ImageBitmap

object OS {
	val platform: Platform = osPlatform // 平台

	object Net {
		fun openUrl(url: String) = osNetOpenUrl(url)
	}

	object Storage {
		val cacheSize get() = osStorageCacheSize
		fun clearCache() = osStorageClearCache()
	}

	object Image {
		fun crop(bitmap: ImageBitmap, startX: Int, startY: Int, width: Int, height: Int) = osImageCrop(bitmap, startX, startY, width, height)
	}
}

// ----  OS

internal expect val osPlatform: Platform

// ------------  Net

internal expect fun osNetOpenUrl(url: String)

// ------------  Storage

internal expect val osStorageCacheSize: Long
internal expect fun osStorageClearCache()

// ------------  Image

internal expect fun osImageCrop(bitmap: ImageBitmap, startX: Int, startY: Int, width: Int, height: Int): ImageBitmap