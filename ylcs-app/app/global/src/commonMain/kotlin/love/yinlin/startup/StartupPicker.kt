package love.yinlin.startup

import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartup
import love.yinlin.foundation.SyncStartupFactory
import love.yinlin.foundation.picker.Picker

class StartupPicker(pool: StartupPool) : SyncStartup(pool) {
    class Factory : SyncStartupFactory<StartupPicker>() {
        override val id: String = StartupID<StartupPicker>()
        override fun build(pool: StartupPool) = StartupPicker(pool)
    }

    val instance = Picker(pool)

    override fun destroy() = instance.close()
}
