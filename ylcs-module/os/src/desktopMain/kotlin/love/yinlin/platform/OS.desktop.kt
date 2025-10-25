package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.common.uri.Uri
import love.yinlin.common.uri.toJvmUri
import love.yinlin.extension.catching
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private external fun requestSingleInstance(): Boolean
private external fun releaseSingleInstance()

fun singleInstance() {
    if (!requestSingleInstance()) {
        releaseSingleInstance()
        exitProcess(0)
    }
    Runtime.getRuntime().addShutdownHook(thread(start = false) { releaseSingleInstance() })
}

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean {
	osNetOpenUrl(uri)
	return true
}

actual fun osApplicationCopyText(text: String): Boolean {
	val clipboard = Toolkit.getDefaultToolkit().systemClipboard
	val selection = StringSelection(text)
	clipboard.setContents(selection, null)
	return true
}

actual fun osNetOpenUrl(uri: Uri) = catching {
	val desktop = Desktop.getDesktop()
	if (desktop.isSupported(Desktop.Action.BROWSE)) {
		desktop.browse(uri.toJvmUri())
	}
}

val osAppPath: Path by lazy {
	val workingDir = Path(System.getProperty("user.dir"))
	val homeDir = Path(System.getProperty("user.home"))
	val appName = "ylcs"
	if (Files.isWritable(Paths.get(workingDir.toString()))) workingDir else {
		when (platform) {
			Platform.Windows -> System.getenv("APPDATA")?.let { Path(it, appName) } ?: workingDir
			Platform.Linux -> Path(System.getenv("XDG_DATA_HOME")?.let { Path(it) } ?: Path(homeDir, ".local", "share"), appName)
			Platform.MacOS -> Path(homeDir, "Library", "Application Support", appName)
			else -> workingDir
		}
	}
}

actual val osStorageDataPath: Path by lazy { Path(osAppPath, "data") }

actual val osStorageCachePath: Path by lazy { Path(System.getProperty("java.io.tmpdir"), "ylcs") }

actual val osStorageCacheSize: Long get() {
	// TODO:
	return 0L
}

actual fun osStorageClearCache() {

}