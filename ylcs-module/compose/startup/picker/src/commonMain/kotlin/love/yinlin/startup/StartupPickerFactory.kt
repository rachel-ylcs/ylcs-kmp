package love.yinlin.startup

import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartupFactory

class StartupPickerFactory : SyncStartupFactory<StartupPicker>() {
    override val id: String = StartupID<StartupPicker>()
    override fun build(pool: StartupPool): StartupPicker = StartupPicker(pool)
}