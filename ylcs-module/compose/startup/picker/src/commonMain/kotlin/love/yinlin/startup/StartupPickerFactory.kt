package love.yinlin.startup

import love.yinlin.foundation.StartupFactory
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool

class StartupPickerFactory : StartupFactory<StartupPicker> {
    override val id: String = StartupID<StartupPicker>()
    override val dependencies: List<String> = emptyList()
    override fun build(pool: StartupPool): StartupPicker = StartupPicker(pool)
}