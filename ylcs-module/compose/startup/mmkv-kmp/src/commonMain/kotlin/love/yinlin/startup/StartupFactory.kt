package love.yinlin.startup

import love.yinlin.coroutines.cpuContext
import love.yinlin.foundation.StartupFactory
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.fs.File
import kotlin.coroutines.CoroutineContext

class StartupKVFactory(private val initPath: File) : StartupFactory<StartupKV> {
    override val id: String = StartupID<StartupKV>()
    override val dependencies: List<String> = emptyList()
    override val dispatcher: CoroutineContext = cpuContext
    override fun build(pool: StartupPool): StartupKV = StartupKV(pool, initPath)
}