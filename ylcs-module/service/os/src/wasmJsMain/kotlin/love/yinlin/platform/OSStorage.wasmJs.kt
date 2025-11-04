package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.Context

actual fun buildOSStorage(context: Context, appName: String): OSStorage = object : OSStorage() {
    override val appPath: Path get() = unsupportedPlatform()

    override val dataPath: Path get() = unsupportedPlatform()

    override val cachePath: Path get() = unsupportedPlatform()

    override suspend fun calcCacheSize(): Long = 0L

    override suspend fun clearCache() { }
}