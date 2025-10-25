package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.service.PlatformContext

actual fun buildOSStorage(context: PlatformContext, appName: String): OSStorage = object : OSStorage() {
    override val dataPath: Path get() = unsupportedPlatform()

    override val cachePath: Path get() = unsupportedPlatform()

    override val cacheSize: Long = 0L

    override fun clearCache() {}
}