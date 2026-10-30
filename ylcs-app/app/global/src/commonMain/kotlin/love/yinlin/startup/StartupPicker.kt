package love.yinlin.startup

import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartup
import love.yinlin.foundation.SyncStartupFactory
import love.yinlin.picker.PickerManager

class StartupPicker(pool: StartupPool) : SyncStartup(pool) {
    class Factory : SyncStartupFactory<StartupPicker>() {
        override val id: String = StartupID<StartupPicker>()
        override fun build(pool: StartupPool) = StartupPicker(pool)
    }

    init {
        PickerManager.initialize(pool)
    }

    val picker get() = PickerManager.picker

    override fun destroy() = PickerManager.shutdown()
}
