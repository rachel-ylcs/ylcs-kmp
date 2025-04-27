@file:JvmName("OSDesktop")
package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.Local
import love.yinlin.common.Uri
import java.awt.Desktop
import java.net.URI

actual val osPlatform: Platform = System.getProperty("os.name").let {
	when {
		it.startsWith("Windows") -> Platform.Windows
		it.startsWith("Mac") -> Platform.MacOS
		else -> Platform.Linux
	}
}

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean = false

actual fun osNetOpenUrl(url: String) {
	try {
		val desktop = Desktop.getDesktop()
		if (desktop.isSupported(Desktop.Action.BROWSE)) {
			desktop.browse(URI(url))
		}
	}
	catch (_: Exception) { }
}

actual val osStorageDataPath: Path get() = Path(System.getProperty("user.dir"), "data")

actual val osStorageCachePath: Path get() = Path(System.getProperty("java.io.tmpdir"), Local.APP_NAME)