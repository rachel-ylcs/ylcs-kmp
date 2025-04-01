@file:JvmName("OSDesktop")

package love.yinlin.platform

import java.awt.Desktop
import java.net.URI

actual val osPlatform: Platform = System.getProperty("os.name").let {
	when {
		it.startsWith("Windows") -> Platform.Windows
		it.startsWith("Mac") -> Platform.MacOS
		else -> Platform.Linux
	}
}

actual fun osNetOpenUrl(url: String) {
	try {
		val desktop = Desktop.getDesktop()
		if (desktop.isSupported(Desktop.Action.BROWSE)) {
			desktop.browse(URI(url))
		}
	}
	catch (_: Exception) { }
}