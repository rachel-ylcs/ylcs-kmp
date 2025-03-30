package love.yinlin.platform

import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.ui.component.screen.DialogProgressState

object OS {
	val platform: Platform = osPlatform // 平台

	object Net {
		fun openUrl(url: String) = osOpenUrl(url)
		suspend fun downloadImage(url: String, state: DialogProgressState) = osDownloadImage(url, state)
	}

	object Storage {
		val cacheSize get() = osCacheSize
		fun clearCache() = osClearCache()
	}

	object Image {
		fun crop(bitmap: ImageBitmap, startX: Int, startY: Int, width: Int, height: Int) = osCrop(bitmap, startX, startY, width, height)
	}
}

// ----  OS

internal expect val osPlatform: Platform

// ------------  Net

internal expect fun osOpenUrl(url: String)
internal expect suspend fun osDownloadImage(url: String, state: DialogProgressState)

// ------------  Storage

internal expect val osCacheSize: Long
internal expect fun osClearCache()

// ------------  Image

internal expect fun osCrop(bitmap: ImageBitmap, startX: Int, startY: Int, width: Int, height: Int): ImageBitmap