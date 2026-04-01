package love.yinlin.startup

import love.yinlin.coroutines.cpuContext
import love.yinlin.foundation.StartupFactory
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import kotlin.coroutines.CoroutineContext

class StartupPickerFactory : StartupFactory<StartupPicker> {
    override val id: String = StartupID<StartupPicker>()
    override val dependencies: List<String> = emptyList()
    override val dispatcher: CoroutineContext = cpuContext
    override fun build(pool: StartupPool): StartupPicker = StartupPicker(pool)
}