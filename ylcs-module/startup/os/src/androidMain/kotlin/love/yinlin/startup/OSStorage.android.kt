package love.yinlin.startup

import kotlinx.io.files.Path
import love.yinlin.foundation.Context
import love.yinlin.fs.deleteRecursively
import love.yinlin.fs.mkdir
import love.yinlin.fs.size

actual fun buildOSStorage(context: Context, appName: String): OSStorage = object : OSStorage() {
    override val appPath: Path = Path(context.application.dataDir.absolutePath)

    override val dataPath: Path = Path(context.application.filesDir.absolutePath)

    override val cachePath: Path = Path(context.application.cacheDir.absolutePath, "temp")

    override suspend fun calcCacheSize(): Long = cachePath.parent?.size() ?: 0L

    override suspend fun clearCache() {
        cachePath.deleteRecursively()
        cachePath.mkdir()
    }
}