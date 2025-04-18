@file:JvmName("OSCommon")
package love.yinlin.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.extension.DateEx
import love.yinlin.ui.component.image.MiniIcon
import kotlin.jvm.JvmName

val UnsupportedPlatformText = "不支持的平台 ${OS.platform}"

open class UnsupportedPlatformException : Exception(UnsupportedPlatformText)

fun unsupportedPlatform(): Nothing = throw UnsupportedPlatformException()

@Composable
fun UnsupportedComponent(modifier: Modifier = Modifier) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
	) {
		MiniIcon(
			icon = Icons.Filled.NotificationImportant,
			size = 50.dp
		)
		Text(text = UnsupportedPlatformText)
	}
}

object OS {
	val platform: Platform = osPlatform // 平台

	inline fun runAndroid(notAndroid: () -> Unit = {}, android: () -> Unit) {
		if (platform == Platform.Android) android()
		else notAndroid()
	}

	inline fun runNotAndroid(android: () -> Unit = {}, notAndroid: () -> Unit) = runAndroid(notAndroid, android)

	inline fun runWeb(notWeb: () -> Unit = {}, web: () -> Unit) {
		if (platform.isWeb) web()
		else notWeb()
	}

	inline fun runNotWeb(web: () -> Unit = {}, notWeb: () -> Unit) = runWeb(notWeb, web)

	inline fun <reified R> ifWeb(notWeb: () -> R, web: () -> R) = if (platform.isWeb) notWeb() else web()

	inline fun <reified R> ifNotWeb(web: () -> R, notWeb: () -> R) = ifWeb(notWeb, web)

	inline fun runPhone(notPhone: () -> Unit = {}, phone: () -> Unit) {
		if (platform.isPhone) phone()
		else notPhone()
	}

	inline fun runNotPhone(phone: () -> Unit = {}, notPhone: () -> Unit) = runPhone(notPhone, phone)

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