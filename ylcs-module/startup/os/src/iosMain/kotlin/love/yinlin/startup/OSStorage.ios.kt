package love.yinlin.startup

import kotlinx.io.files.Path
import love.yinlin.foundation.Context
import love.yinlin.fs.deleteRecursively
import love.yinlin.fs.mkdir
import love.yinlin.fs.size
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun buildOSStorage(context: Context, appName: String): OSStorage = object : OSStorage() {
    private fun searchPath(directory: NSSearchPathDirectory): Path {
        val paths = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true)
        return Path(paths[0]!! as String)
    }

    override val appPath: Path = Path(NSHomeDirectory())

    override val dataPath: Path = searchPath(NSDocumentDirectory)

    override val cachePath: Path = Path(searchPath(NSCachesDirectory), "temp")

    override suspend fun calcCacheSize(): Long = cachePath.parent?.size() ?: 0L

    override suspend fun clearCache() {
        cachePath.deleteRecursively()
        cachePath.mkdir()
    }
}