package love.yinlin.platform

import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.ui.component.screen.DialogProgressState

object OS {
	val platform: Platform = osPlatform // 平台

	object Net {
		fun openUrl(url: String) = osNetOpenUrl(url)
		suspend fun downloadImage(url: String, state: DialogProgressState) = osNetDownloadImage(url, state)
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
internal expect suspend fun osNetDownloadImage(url: String, state: DialogProgressState)

// ------------  Storage

internal expect val osStorageCacheSize: Long
internal expect fun osStorageClearCache()

// ------------  Image

internal expect fun osImageCrop(bitmap: ImageBitmap, startX: Int, startY: Int, width: Int, height: Int): ImageBitmap

expect fun test(block: (ByteArray) -> Unit)