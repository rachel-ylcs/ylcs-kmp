package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.Context
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun buildOSStorage(context: Context, appName: String): OSStorage = object : OSStorage() {
    private fun searchPath(directory: NSSearchPathDirectory): Path {
        val paths = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true)
        return Path(paths[0]!! as String)
    }

    override val dataPath: Path = searchPath(NSDocumentDirectory)

    override val cachePath: Path = searchPath(NSCachesDirectory)

    override val cacheSize: Long get() {
        // TODO:
        return 0L
    }

    override fun clearCache() {

    }
}