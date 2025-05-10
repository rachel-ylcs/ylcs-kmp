@file:JvmName("OSDesktop")
package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.Local
import love.yinlin.common.Uri
import net.harawata.appdirs.AppDirsFactory
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths

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

val osAppDataPath: Path get() {
	val workingDir = Path(System.getProperty("user.dir"))
	return if (Files.isWritable(Paths.get(workingDir.toString()))) {
		workingDir
	} else {
		Path(AppDirsFactory.getInstance().getUserDataDir(Local.APP_NAME, null, null, true))
	}
}

actual val osStorageDataPath: Path get() = Path(osAppDataPath, "data")

actual val osStorageCachePath: Path get() = Path(System.getProperty("java.io.tmpdir"), Local.APP_NAME)