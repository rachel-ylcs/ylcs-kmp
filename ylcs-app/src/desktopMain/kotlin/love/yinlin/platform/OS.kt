@file:JvmName("OSDesktop")
package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.Local
import love.yinlin.common.Uri
import love.yinlin.extension.catching
import net.harawata.appdirs.AppDirsFactory
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean {
	osNetOpenUrl(uri.toString())
	return true
}

actual fun osApplicationCopyText(text: String): Boolean {
	val clipboard = Toolkit.getDefaultToolkit().systemClipboard
	val selection = StringSelection(text)
	clipboard.setContents(selection, null)
	return true
}

actual fun osNetOpenUrl(url: String) = catching {
	val desktop = Desktop.getDesktop()
	if (desktop.isSupported(Desktop.Action.BROWSE)) {
		desktop.browse(URI(url))
	}
}

val osAppPath: Path get() {
	val workingDir = Path(System.getProperty("user.dir"))
	return if (Files.isWritable(Paths.get(workingDir.toString()))) workingDir
		else Path(AppDirsFactory.getInstance().getUserDataDir(Local.APP_NAME, null, null, true))
}

actual val osStorageDataPath: Path get() = Path(osAppPath, "data")

actual val osStorageCachePath: Path get() = Path(System.getProperty("java.io.tmpdir"), Local.APP_NAME)