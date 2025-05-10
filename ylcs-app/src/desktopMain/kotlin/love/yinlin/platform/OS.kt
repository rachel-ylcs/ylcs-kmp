@file:JvmName("OSDesktop")
package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.Local
import love.yinlin.common.Uri
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

val osAppPath: Path get() {
	val workingDir = Path(System.getProperty("user.dir"))
	val homeDir = Path(System.getProperty("user.home"))
	return if (Files.isWritable(Paths.get(workingDir.toString()))) workingDir else {
		when (osPlatform) {
            Platform.Windows -> System.getenv("APPDATA")?.let { Path(it, Local.APP_NAME) } ?: workingDir
            Platform.Linux -> Path(System.getenv("XDG_DATA_HOME")?.let { Path(it) }
				?: Path(homeDir, ".local", "share"), Local.APP_NAME)
            Platform.MacOS -> Path(homeDir, "Library", "Application Support", Local.APP_NAME)
            else -> workingDir
        }
	}
}

actual val osStorageDataPath: Path get() = Path(osAppPath, "data")

actual val osStorageCachePath: Path get() = Path(System.getProperty("java.io.tmpdir"), Local.APP_NAME)