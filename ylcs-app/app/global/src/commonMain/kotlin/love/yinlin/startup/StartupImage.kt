package love.yinlin.startup

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.ImageManager
import love.yinlin.compose.image.data.ImageConfiguration
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartup
import love.yinlin.foundation.SyncStartupFactory

@Stable
class StartupImage(pool: StartupPool, appName: String, configuration: ImageConfiguration) : SyncStartup(pool) {
    class Factory(
        private val appName: String = "Rachel",
        private val configuration: ImageConfiguration = ImageConfiguration(),
    ) : SyncStartupFactory<StartupImage>() {
        override val id: String = StartupID<StartupImage>()
        override fun build(pool: StartupPool): StartupImage = StartupImage(pool, appName, configuration)
    }

    init {
        ImageManager.initialize(pool.rawContext, appName, configuration)
    }

    suspend fun clearCache() = ImageManager.clearCache()

    override fun destroy() = ImageManager.shutdown()
}