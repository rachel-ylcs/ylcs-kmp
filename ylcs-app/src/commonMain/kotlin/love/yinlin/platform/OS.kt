package love.yinlin.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.ThemeValue
import love.yinlin.common.Uri
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingNull
import love.yinlin.ui.component.image.MiniIcon

val UnsupportedPlatformText = "不支持的平台 $platform"

open class UnsupportedPlatformException : Exception(UnsupportedPlatformText)

fun unsupportedPlatform(): Nothing = throw UnsupportedPlatformException()

@Composable
fun UnsupportedComponent(modifier: Modifier = Modifier) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace, Alignment.CenterVertically)
	) {
		MiniIcon(
			icon = Icons.Filled.NotificationImportant,
			size = ThemeValue.Size.Image
		)
		Text(text = UnsupportedPlatformText)
	}
}

@Stable
object OS {
	fun platform(vararg filter: Platform): Boolean = filter.contains(platform)

	fun notPlatform(vararg filter: Platform): Boolean = !filter.contains(platform)

	inline fun ifPlatform(vararg filter: Platform, block: () -> Unit) = if (platform(*filter)) block() else Unit

	inline fun <T> ifPlatform(vararg filter: Platform, ifTrue: () -> T, ifFalse: () -> T): T = if (platform(*filter)) ifTrue() else ifFalse()

	inline fun ifNotPlatform(vararg filter: Platform, block: () -> Unit) = if (notPlatform(*filter)) block() else Unit

	inline fun <T> ifNotPlatform(vararg filter: Platform, ifTrue: () -> T, ifFalse: () -> T): T = if (notPlatform(*filter)) ifTrue() else ifFalse()

	@Stable
	object Application {
		suspend fun startAppIntent(uri: Uri): Boolean = osApplicationStartAppIntent(uri)
		fun copyText(text: String): Boolean = osApplicationCopyText(text)
	}

	@Stable
	object Net {
		fun openUrl(url: String) = osNetOpenUrl(url)
	}

	@Stable
	object Storage {
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

internal expect fun osNetOpenUrl(url: String)

// ------------  Storage

internal expect val osStorageDataPath: Path
internal expect val osStorageCachePath: Path
internal expect val osStorageCacheSize: Long
internal expect fun osStorageClearCache()