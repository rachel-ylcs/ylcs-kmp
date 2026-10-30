package love.yinlin.startup

import love.yinlin.data.config.AppConfig
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartup
import love.yinlin.foundation.SyncStartupFactory

class StartupAppConfig(pool: StartupPool) : SyncStartup(pool) {
    class Factory : SyncStartupFactory<StartupAppConfig>() {
        override val id: String = StartupID<StartupAppConfig>()
        override val dependencies: List<String> = [StartupID<StartupKV>()]
        override fun build(pool: StartupPool) = StartupAppConfig(pool)
    }

    val config = AppConfig(pool.requireClass<StartupKV>().kv)
}
